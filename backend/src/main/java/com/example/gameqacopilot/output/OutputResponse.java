package com.example.gameqacopilot.output;

import java.time.LocalDateTime;

public record OutputResponse(
        Long id,
        Long projectId,
        Long planningDocumentId,
        String outputType,
        OutputStatus status,
        String finalContent,
        String fileName,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String failureReason) {

    static OutputResponse from(Output output) {
        return new OutputResponse(
                output.getId(), output.getProjectId(), output.getPlanningDocumentId(), output.getOutputType(),
                output.getStatus(), output.getFinalContent(), output.getFileName(), output.getCreatedAt(),
                output.getCompletedAt(), output.getFailureReason());
    }
}
