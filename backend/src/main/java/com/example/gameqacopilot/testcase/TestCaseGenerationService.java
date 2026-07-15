package com.example.gameqacopilot.testcase;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.analysis.dto.TestCaseGenerationResponse;
import com.example.gameqacopilot.analysis.service.AnalysisJobService;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class TestCaseGenerationService {
    private final AnalysisJobService jobs;
    private final RequirementRepository requirements;
    private final TestCaseRepository testCases;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Resource systemPrompt;
    private final Resource testCasePrompt;

    public TestCaseGenerationService(AnalysisJobService jobs, RequirementRepository requirements,
            TestCaseRepository testCases, ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
            @Value("classpath:/prompts/v1.0/system.txt") Resource systemPrompt,
            @Value("classpath:/prompts/v1.0/test-case.txt") Resource testCasePrompt) {
        this.jobs = jobs;
        this.requirements = requirements;
        this.testCases = testCases;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.systemPrompt = systemPrompt;
        this.testCasePrompt = testCasePrompt;
    }

    public TestCaseGenerationResponse generate(Long jobId,
            List<AiAnalysisResponse.CategoryTree> categoryTree,
            List<AiAnalysisResponse.Requirement> requirementValues) {
        try {
            return generate(jobId, jobs.prepareRequirements(jobId).job(), categoryTree, requirementValues);
        } catch (RuntimeException exception) {
            jobs.fail(jobId, exception.getMessage());
            throw exception;
        }
    }

    private TestCaseGenerationResponse generate(Long jobId,
            com.example.gameqacopilot.analysis.entity.AnalysisJob job,
            List<AiAnalysisResponse.CategoryTree> categories,
            List<AiAnalysisResponse.Requirement> requirementValues) {
        return call(jobId, job, categories, request(categories, requirementValues));
    }

    private String request(List<AiAnalysisResponse.CategoryTree> categories,
            List<AiAnalysisResponse.Requirement> requirements) {
        try {
            return testCasePrompt.getContentAsString(StandardCharsets.UTF_8)
                + "\n\nCategory tree JSON:\n" + objectMapper.writeValueAsString(categories)
                + "\n\nRequirements JSON:\n" + objectMapper.writeValueAsString(requirements);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Test case prompt failed", exception);
        }
    }

    private TestCaseGenerationResponse call(Long jobId,
            com.example.gameqacopilot.analysis.entity.AnalysisJob job,
            List<AiAnalysisResponse.CategoryTree> categories, String request) {
        var response = chatClient.prompt().system(systemPrompt).user(request).call()
                .responseEntity(TestCaseGenerationResponse.class);
        var byExternalId = requirements.findAllByAnalysisJob_Id(jobId).stream()
                .collect(Collectors.toMap(Requirement::getExternalRequirementId, Function.identity()));
        validate(response.entity(), categories, byExternalId);
        testCases.saveAll(response.entity().testCases().stream()
                .map(value -> toEntity(job, byExternalId.get(value.requirementId()), value)).toList());
        var chatResponse = response.response();
        var usage = chatResponse.getMetadata().getUsage();
        jobs.recordTestCases(jobId, chatResponse.getResult().getOutput().getText(),
                usage == null || usage.getTotalTokens() == null ? null : usage.getTotalTokens().longValue());
        return response.entity();
    }

    private void validate(TestCaseGenerationResponse response,
            List<AiAnalysisResponse.CategoryTree> categories,
            Map<String, Requirement> requirements) {
        if (response == null || response.testCases() == null || response.testCases().isEmpty()) {
            throw new IllegalArgumentException("Test cases are empty");
        }
        for (var value : response.testCases()) {
            if (!knownCategory(categories, value)
                    || !matchesRequirement(requirements.get(value.requirementId()), value)) {
                throw new IllegalArgumentException("Invalid test case category or requirement");
            }
            validateRequired(value);
        }
    }

    private boolean knownCategory(List<AiAnalysisResponse.CategoryTree> categories,
            AiAnalysisResponse.TestCase value) {
        return categories.stream().anyMatch(major ->
                major.majorCategory().equals(value.majorCategory())
                && major.middleCategories().stream().anyMatch(middle ->
                        middle.name().equals(value.middleCategory())
                        && middle.minorCategories().contains(value.minorCategory())));
    }

    private boolean matchesRequirement(Requirement requirement, AiAnalysisResponse.TestCase value) {
        return requirement != null
                && requirement.getMajorCategory().equals(value.majorCategory())
                && requirement.getMiddleCategory().equals(value.middleCategory())
                && requirement.getMinorCategory().equals(value.minorCategory());
    }

    private void validateRequired(AiAnalysisResponse.TestCase value) {
        if (value.testCaseId() == null || value.testCaseId().isBlank() || value.displayOrder() < 1
                || value.evidences() == null || value.evidences().isEmpty()) {
            throw new IllegalArgumentException("Test case ID, order and evidence are required");
        }
    }

    private TestCase toEntity(com.example.gameqacopilot.analysis.entity.AnalysisJob job,
            Requirement requirement, AiAnalysisResponse.TestCase value) {
        return new TestCase(job, requirement, value,
                json(value.preconditions()), json(value.testSteps()), json(value.expectedResults()),
                json(value.evidences()), json(value.notes()));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Test case JSON serialization failed", exception);
        }
    }
}
