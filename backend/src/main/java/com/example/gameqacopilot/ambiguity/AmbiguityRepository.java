package com.example.gameqacopilot.ambiguity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbiguityRepository extends JpaRepository<Ambiguity, Long> {
    List<Ambiguity> findAllByPlanningDocument_IdOrderById(Long planningDocumentId);
}
