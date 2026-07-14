package com.example.gameqacopilot.document.repository;

import com.example.gameqacopilot.document.entity.PlanningDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanningDocumentRepository extends JpaRepository<PlanningDocument, Long> {
    List<PlanningDocument> findAllByProject_IdOrderByCreatedAtDesc(Long projectId);
}
