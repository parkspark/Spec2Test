package com.example.gameqacopilot.testcase;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.project.ProjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCaseQueryService {
    private final TestCaseRepository testCases;
    private final ProjectRepository projects;
    private final ObjectMapper objectMapper;

    public TestCaseQueryService(
            TestCaseRepository testCases, ProjectRepository projects, ObjectMapper objectMapper) {
        this.testCases = testCases;
        this.projects = projects;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TestCaseListResponse findAll(
            Long projectId,
            Long analysisId,
            TestCaseStatus status,
            String majorCategory,
            String middleCategory,
            String minorCategory,
            AiAnalysisResponse.TestType testType,
            AiAnalysisResponse.Confidence confidence,
            String keyword) {
        if (!projects.existsById(projectId)) throw new NoSuchElementException("Project not found");
        var items = testCases.findAllFiltered(projectId, analysisId, status,
                        value(majorCategory), value(middleCategory), value(minorCategory),
                        testType, confidence, value(keyword)).stream()
                .map(this::response)
                .toList();
        return new TestCaseListResponse(items);
    }

    @Transactional(readOnly = true)
    public TestCaseResponse findById(Long id) {
        return response(testCases.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Test case not found")));
    }

    TestCaseResponse response(TestCase testCase) {
        List<String> preconditions = read(testCase.getPreconditions(), new TypeReference<>() {});
        List<AiAnalysisResponse.TestStep> steps = read(testCase.getTestSteps(), new TypeReference<>() {});
        List<String> expectedResults = read(testCase.getExpectedResults(), new TypeReference<>() {});
        List<AiAnalysisResponse.Evidence> evidences = read(testCase.getEvidences(), new TypeReference<>() {});
        List<String> notes = read(testCase.getNotes(), new TypeReference<>() {});
        return new TestCaseResponse(
                testCase.getId(), testCase.getExternalTestCaseId(), testCase.getAnalysisJobId(),
                testCase.getRequirementId(), testCase.getDisplayOrder(), testCase.getMajorCategory(),
                testCase.getMiddleCategory(), testCase.getMinorCategory(), testCase.getTestItem(),
                testCase.getTestType(), testCase.getPriority(), testCase.getConfidence(), testCase.getStatus(),
                preconditions, steps, expectedResults,
                TestCaseResponse.EvidenceSummary.from(evidences.stream().findFirst().orElse(null)),
                evidences, notes, testCase.isRequiresHumanReview(), testCase.getReviewedById(),
                testCase.getReviewedAt(), testCase.getRejectionReason());
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored test case data is invalid", exception);
        }
    }

    private String value(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
