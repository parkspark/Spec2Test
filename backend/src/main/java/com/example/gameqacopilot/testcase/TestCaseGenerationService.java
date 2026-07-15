package com.example.gameqacopilot.testcase;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.analysis.dto.TestCaseGenerationResponse;
import com.example.gameqacopilot.analysis.service.AnalysisJobService;
import com.example.gameqacopilot.analysis.validator.AnalysisResultValidator;
import com.example.gameqacopilot.evidence.EvidenceVerifier;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
public class TestCaseGenerationService {
    private final AnalysisJobService jobs;
    private final RequirementRepository requirements;
    private final TestCaseRepository testCases;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Resource systemPrompt;
    private final Resource testCasePrompt;
    private final EvidenceVerifier evidenceVerifier;

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
        this.evidenceVerifier = new EvidenceVerifier(objectMapper);
    }

    public TestCaseGenerationResponse generate(Long jobId,
            List<AiAnalysisResponse.CategoryTree> categoryTree,
            List<AiAnalysisResponse.Requirement> requirementValues) {
        try {
            var input = jobs.prepareRequirements(jobId);
            return generate(jobId, input, categoryTree, requirementValues);
        } catch (RuntimeException exception) {
            jobs.fail(jobId, exception.getMessage());
            throw exception;
        }
    }

    private TestCaseGenerationResponse generate(Long jobId,
            AnalysisJobService.RequirementInput input,
            List<AiAnalysisResponse.CategoryTree> categories,
            List<AiAnalysisResponse.Requirement> requirementValues) {
        return call(jobId, input, categories, request(categories, requirementValues));
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
            AnalysisJobService.RequirementInput input,
            List<AiAnalysisResponse.CategoryTree> categories, String request) {
        var byExternalId = requirements.findAllByAnalysisJob_Id(jobId).stream()
                .collect(Collectors.toMap(Requirement::getExternalRequirementId, Function.identity()));
        var response = AnalysisResultValidator.validateWithOneRetry(
                () -> chatClient.prompt().system(systemPrompt).user(request).call()
                        .responseEntity(TestCaseGenerationResponse.class),
                value -> validate(value.entity(), categories, byExternalId));
        testCases.saveAll(response.entity().testCases().stream()
                .map(value -> toEntity(input.job(), byExternalId.get(value.requirementId()), value,
                        evidenceVerifier.verify(value.evidences(), input.document().pageContents(),
                                input.document().pageCount()))).toList());
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
        var ids = new HashSet<String>();
        var orders = new HashSet<Integer>();
        var purposes = new HashSet<String>();
        for (var value : response.testCases()) {
            if (!knownCategory(categories, value)
                    || !matchesRequirement(requirements.get(value.requirementId()), value)) {
                throw new IllegalArgumentException("Invalid test case category or requirement");
            }
            validateRequired(value);
            if (!ids.add(value.testCaseId()) || !orders.add(value.displayOrder())
                    || !purposes.add(String.join("|", value.majorCategory(), value.middleCategory(),
                            value.minorCategory(), value.testItem().strip()))) {
                throw new IllegalArgumentException("Duplicate test case");
            }
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
        if (blank(value.testCaseId()) || value.displayOrder() < 1 || blank(value.testItem())
                || value.testType() == null || blank(value.priority()) || value.confidence() == null
                || value.preconditions() == null || value.notes() == null
                || value.testSteps() == null || value.testSteps().isEmpty()
                || value.testSteps().stream().anyMatch(step -> step.stepNumber() < 1
                        || blank(step.action()) || blank(step.expectedResult()))
                || value.expectedResults() == null || value.expectedResults().isEmpty()
                || value.expectedResults().stream().anyMatch(this::blank)
                || value.evidences() == null || value.evidences().isEmpty()
                || value.evidences().stream().anyMatch(AnalysisResultValidator::invalidEvidence)) {
            throw new IllegalArgumentException("Test case fields and evidence are invalid");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private TestCase toEntity(com.example.gameqacopilot.analysis.entity.AnalysisJob job,
            Requirement requirement, AiAnalysisResponse.TestCase value,
            List<AiAnalysisResponse.Evidence> evidences) {
        return new TestCase(job, requirement, value,
                json(value.preconditions()), json(value.testSteps()), json(value.expectedResults()),
                json(evidences), json(TestCaseNotes.merge(value.notes(), evidences)),
                value.requiresHumanReview() || evidenceVerifier.requiresHumanReview(evidences));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Test case JSON serialization failed", exception);
        }
    }
}
