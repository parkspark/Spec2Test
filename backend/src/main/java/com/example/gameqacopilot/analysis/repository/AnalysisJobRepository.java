package com.example.gameqacopilot.analysis.repository;

import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {
    Optional<AnalysisJob> findFirstByPlanningDocument_IdOrderByCreatedAtDesc(Long documentId);
}
