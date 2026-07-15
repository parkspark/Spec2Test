package com.example.gameqacopilot.ambiguity;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmbiguityRepository extends JpaRepository<Ambiguity, Long> {
    List<Ambiguity> findAllByPlanningDocument_IdOrderById(Long planningDocumentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Ambiguity a where a.id = :id")
    Optional<Ambiguity> findForUpdateById(@Param("id") Long id);
}
