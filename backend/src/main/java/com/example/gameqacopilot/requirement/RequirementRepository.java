package com.example.gameqacopilot.requirement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {
    List<Requirement> findAllByAnalysisJob_Id(Long analysisJobId);

    List<Requirement> findAllByAnalysisJob_PlanningDocument_Id(Long planningDocumentId);
}
