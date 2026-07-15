package com.example.gameqacopilot.testcase;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestCaseController {
    private final TestCaseQueryService testCases;

    public TestCaseController(TestCaseQueryService testCases) {
        this.testCases = testCases;
    }

    @GetMapping("/api/projects/{projectId}/test-cases")
    ApiResponse<TestCaseListResponse> findAll(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long analysisId,
            @RequestParam(required = false) TestCaseStatus status,
            @RequestParam(required = false) String majorCategory,
            @RequestParam(required = false) String middleCategory,
            @RequestParam(required = false) String minorCategory,
            @RequestParam(required = false) AiAnalysisResponse.TestType testType,
            @RequestParam(required = false) AiAnalysisResponse.Confidence confidence,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.of(testCases.findAll(projectId, analysisId, status, majorCategory,
                middleCategory, minorCategory, testType, confidence, keyword));
    }

    @GetMapping("/api/test-cases/{testCaseId}")
    ApiResponse<TestCaseResponse> findById(@PathVariable Long testCaseId) {
        return ApiResponse.of(testCases.findById(testCaseId));
    }
}
