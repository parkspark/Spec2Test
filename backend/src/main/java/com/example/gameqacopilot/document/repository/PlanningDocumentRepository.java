package com.example.gameqacopilot.document.repository;

import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.document.entity.DocumentProcessingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanningDocumentRepository extends JpaRepository<PlanningDocument, Long> {
    List<PlanningDocument> findAllByProject_IdOrderByCreatedAtDesc(Long projectId);

    Optional<PlanningDocument> findFirstByProject_IdAndProcessingStatusOrderByCreatedAtDesc(
            Long projectId, DocumentProcessingStatus processingStatus);
}
