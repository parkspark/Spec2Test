package com.example.gameqacopilot.analysis.controller;

import com.example.gameqacopilot.analysis.dto.AnalysisJobResponse;
import com.example.gameqacopilot.analysis.service.AnalysisJobService;
import com.example.gameqacopilot.common.response.ApiResponse;
import com.example.gameqacopilot.common.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AnalysisJobController {
    private final AnalysisJobService analysisJobs;

    public AnalysisJobController(AnalysisJobService analysisJobs) {
        this.analysisJobs = analysisJobs;
    }

    @PostMapping("/api/documents/{documentId}/analyses")
    ApiResponse<AnalysisJobResponse> request(
            @PathVariable Long documentId, @AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.of(analysisJobs.request(documentId, user.id()));
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
