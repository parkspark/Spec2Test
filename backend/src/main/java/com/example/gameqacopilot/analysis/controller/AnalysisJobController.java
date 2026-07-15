package com.example.gameqacopilot.analysis.controller;

import com.example.gameqacopilot.analysis.dto.AnalysisJobResponse;
import com.example.gameqacopilot.analysis.service.AnalysisJobService;
import com.example.gameqacopilot.analysis.service.CategoryClassificationService;
import com.example.gameqacopilot.common.response.ApiResponse;
import com.example.gameqacopilot.common.security.CurrentUser;
import com.example.gameqacopilot.requirement.RequirementExtractionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AnalysisJobController {
    private final AnalysisJobService analysisJobs;
    private final CategoryClassificationService classifications;
    private final RequirementExtractionService requirements;

    public AnalysisJobController(AnalysisJobService analysisJobs, CategoryClassificationService classifications,
            RequirementExtractionService requirements) {
        this.analysisJobs = analysisJobs;
        this.classifications = classifications;
        this.requirements = requirements;
    }

    @PostMapping("/api/documents/{documentId}/analyses")
    ApiResponse<AnalysisJobResponse> request(
            @PathVariable Long documentId, @AuthenticationPrincipal CurrentUser user) {
        var job = analysisJobs.request(documentId, user.id());
        var classification = classifications.classify(job.id());
        requirements.extract(job.id(), classification.categoryTree());
        return ApiResponse.of(analysisJobs.findById(job.id()));
    }

    @GetMapping("/api/analyses/{analysisId}")
    ApiResponse<AnalysisJobResponse> findById(@PathVariable Long analysisId) {
        return ApiResponse.of(analysisJobs.findById(analysisId));
    }

    @GetMapping("/api/documents/{documentId}/analyses/latest")
    ApiResponse<AnalysisJobResponse> findLatest(@PathVariable Long documentId) {
        return ApiResponse.of(analysisJobs.findLatest(documentId));
    }
}
