package com.example.gameqacopilot.testcase;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.common.response.ApiResponse;
import com.example.gameqacopilot.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
public class TestCaseController {
    private final TestCaseQueryService testCases;
    private final TestCaseReviewService reviews;

    public TestCaseController(TestCaseQueryService testCases, TestCaseReviewService reviews) {
        this.testCases = testCases;
        this.reviews = reviews;
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

    @PostMapping("/api/test-cases/{testCaseId}/approve")
    ApiResponse<TestCaseResponse> approve(
            @PathVariable Long testCaseId, @AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.of(reviews.approve(testCaseId, user.id()));
    }

    @PostMapping("/api/test-cases/{testCaseId}/reject")
    ApiResponse<TestCaseResponse> reject(
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseRejectRequest request,
            @AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.of(reviews.reject(testCaseId, user.id(), request.reason()));
    }
}
