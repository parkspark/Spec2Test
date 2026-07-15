package com.example.gameqacopilot.analysis.dto;

import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import com.example.gameqacopilot.analysis.entity.AnalysisJobStatus;
import java.time.LocalDateTime;

public record AnalysisJobResponse(
        Long id,
        Long planningDocumentId,
        AnalysisJobStatus status,
        String modelName,
        String promptVersion,
        Long requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String failureReason,
        Long tokenUsage,
        String rawResponse,
        LocalDateTime createdAt) {
    public static AnalysisJobResponse from(AnalysisJob job) {
        return new AnalysisJobResponse(
                job.getId(), job.getPlanningDocumentId(), job.getStatus(), job.getModelName(),
                job.getPromptVersion(), job.getRequestedById(), job.getRequestedAt(), job.getStartedAt(),
                job.getCompletedAt(), job.getFailureReason(), job.getTokenUsage(), job.getRawResponse(),
                job.getCreatedAt());
    }
}
