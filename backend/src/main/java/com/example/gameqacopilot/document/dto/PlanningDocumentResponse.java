package com.example.gameqacopilot.document.dto;

import com.example.gameqacopilot.document.entity.DocumentProcessingStatus;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import java.time.LocalDateTime;

public record PlanningDocumentResponse(
        Long id, Long projectId, String title, String originalFileName, String mimeType, long fileSize,
        Integer pageCount, DocumentProcessingStatus processingStatus, String failureReason,
        Long createdBy, LocalDateTime createdAt) {
    public static PlanningDocumentResponse from(PlanningDocument document) {
        return new PlanningDocumentResponse(
                document.getId(), document.getProjectId(), document.getTitle(), document.getOriginalFileName(),
                document.getMimeType(), document.getFileSize(), document.getPageCount(),
                document.getProcessingStatus(), document.getFailureReason(), document.getCreatedById(),
                document.getCreatedAt());
    }
}
