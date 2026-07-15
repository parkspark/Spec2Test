package com.example.gameqacopilot.ambiguity;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.analysis.dto.AmbiguityGenerationResponse;
import com.example.gameqacopilot.analysis.service.AnalysisJobService;
import com.example.gameqacopilot.analysis.validator.AnalysisResultValidator;
import com.example.gameqacopilot.evidence.EvidenceVerifier;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.example.gameqacopilot.testcase.TestCase;
import com.example.gameqacopilot.testcase.TestCaseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AmbiguityGenerationService {
    private final AnalysisJobService jobs;
    private final RequirementRepository requirements;
    private final TestCaseRepository testCases;
    private final AmbiguityRepository ambiguities;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Resource systemPrompt;
    private final Resource ambiguityPrompt;
    private final EvidenceVerifier evidenceVerifier;

    public AmbiguityGenerationService(AnalysisJobService jobs, RequirementRepository requirements,
            TestCaseRepository testCases, AmbiguityRepository ambiguities,
            ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
            @Value(\u0022classpath:/prompts/v1.0/system.txt\u0022) Resource systemPrompt,
            @Value(\u0022classpath:/prompts/v1.0/ambiguity.txt\u0022) Resource ambiguityPrompt) {
        this.jobs = jobs;
        this.requirements = requirements;
        this.testCases = testCases;
        this.ambiguities = ambiguities;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.systemPrompt = systemPrompt;
        this.ambiguityPrompt = ambiguityPrompt;
        this.evidenceVerifier = new EvidenceVerifier(objectMapper);
    }

    public AmbiguityGenerationResponse generate(Long jobId,
            List<AiAnalysisResponse.Requirement> requirementValues,
            List<AiAnalysisResponse.TestCase> testCaseValues) {
        try {
            var input = jobs.prepareRequirements(jobId);
            var job = input.job();
            var byExternalId = requirements.findAllByAnalysisJob_Id(jobId).stream()
                    .collect(Collectors.toMap(Requirement::getExternalRequirementId, Function.identity()));
            String request = request(requirementValues, testCaseValues);
            var response = AnalysisResultValidator.validateWithOneRetry(
                    () -> chatClient.prompt().system(systemPrompt).user(request).call()
                            .responseEntity(AmbiguityGenerationResponse.class),
                    value -> validate(value.entity(), byExternalId));
            ambiguities.saveAll(response.entity().ambiguities().stream()
                    .map(value -> new Ambiguity(job, value, json(value.relatedRequirementIds()),
                            json(evidenceVerifier.verify(value.evidences(), input.document().pageContents(),
                                    input.document().pageCount())))).toList());
            linkTestCases(jobId, response.entity().ambiguities());
            var chatResponse = response.response();
            var usage = chatResponse.getMetadata().getUsage();
            jobs.recordAmbiguities(jobId, chatResponse.getResult().getOutput().getText(),
                    usage == null || usage.getTotalTokens() == null ? null : usage.getTotalTokens().longValue());
            return response.entity();
        } catch (Exception exception) {
            jobs.fail(jobId, exception.getMessage());
            throw new IllegalStateException(exception);
        }
    }
    private void validate(AmbiguityGenerationResponse response, Map<String, Requirement> requirements) {
        if (response == null || response.ambiguities() == null) {
            throw new IllegalArgumentException(\u0022Ambiguities are missing\u0022);
        }
        var ids = new HashSet<String>();
        for (var value : response.ambiguities()) {
            if (blank(value.ambiguityId()) || blank(value.title()) || blank(value.description())
                    || blank(value.question()) || blank(value.impact()) || blank(value.severity())
                    || !ids.add(value.ambiguityId())
                    || value.relatedRequirementIds() == null || value.relatedRequirementIds().isEmpty()
                    || value.evidences() == null || value.evidences().isEmpty()
                    || value.evidences().stream().anyMatch(AnalysisResultValidator::invalidEvidence)) {
                throw new IllegalArgumentException(\u0022Ambiguity fields, requirements and evidence are required\u0022);
            }
            var related = value.relatedRequirementIds().stream().map(requirements::get).toList();
            if (related.contains(null) || related.stream().noneMatch(requirement ->
                    requirement.getMajorCategory().equals(value.majorCategory())
                    && requirement.getMiddleCategory().equals(value.middleCategory())
                    && requirement.getMinorCategory().equals(value.minorCategory()))) {
                throw new IllegalArgumentException(\u0022Ambiguity requirement or category is invalid\u0022);
            }
        }
    }

    private void linkTestCases(Long jobId, List<AiAnalysisResponse.Ambiguity> values) {
        var cases = testCases.findAllWithRequirementByAnalysisJobId(jobId);
        for (var value : values) {
            cases.stream().filter(testCase ->
                    value.relatedRequirementIds().contains(testCase.getRequirementExternalId()))
                    .forEach(testCase -> appendNotes(testCase, value.ambiguityId()));
        }
        testCases.saveAll(cases);
    }

    private void appendNotes(TestCase testCase, String ambiguityId) {
        try {
            var notes = new ArrayList<>(objectMapper.readValue(
                    testCase.getNotes(), new TypeReference<List<String>>() {}));
            addOnce(notes, \u0022관련 모호성: \u0022 + ambiguityId);
            addOnce(notes, \u0022기획 확인 필요\u0022);
            testCase.updateNotesForAmbiguity(objectMapper.writeValueAsString(notes));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(\u0022Test case notes are invalid\u0022, exception);
        }
    }

    private void addOnce(List<String> notes, String note) {
        if (!notes.contains(note)) notes.add(note);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(\u0022Ambiguity evidence serialization failed\u0022, exception);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String request(List<AiAnalysisResponse.Requirement> requirements,
            List<AiAnalysisResponse.TestCase> testCases) throws Exception {
        String line = System.lineSeparator();
        return ambiguityPrompt.getContentAsString(StandardCharsets.UTF_8)
                + line + line + \u0022Requirements JSON:\u0022 + line + objectMapper.writeValueAsString(requirements)
                + line + line + \u0022Test cases JSON:\u0022 + line + objectMapper.writeValueAsString(testCases);
    }
}
