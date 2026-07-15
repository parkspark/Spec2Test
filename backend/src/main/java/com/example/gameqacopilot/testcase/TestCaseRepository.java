package com.example.gameqacopilot.testcase;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findAllByAnalysisJob_Id(Long analysisJobId);

    List<TestCase> findAllByProject_IdOrderByDisplayOrder(Long projectId);

    List<TestCase> findAllByPlanningDocument_IdAndStatusOrderByDisplayOrder(
            Long planningDocumentId, TestCaseStatus status);

    @Query("""
            SELECT t FROM TestCase t
            WHERE t.project.id = :projectId
              AND (:analysisId IS NULL OR t.analysisJob.id = :analysisId)
              AND (:status IS NULL OR t.status = :status)
              AND (:majorCategory IS NULL OR t.majorCategory = :majorCategory)
              AND (:middleCategory IS NULL OR t.middleCategory = :middleCategory)
              AND (:minorCategory IS NULL OR t.minorCategory = :minorCategory)
              AND (:testType IS NULL OR t.testType = :testType)
              AND (:confidence IS NULL OR t.confidence = :confidence)
              AND (:keyword IS NULL OR LOWER(t.testItem) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY t.displayOrder
            """)
    List<TestCase> findAllFiltered(
            @Param("projectId") Long projectId,
            @Param("analysisId") Long analysisId,
            @Param("status") TestCaseStatus status,
            @Param("majorCategory") String majorCategory,
            @Param("middleCategory") String middleCategory,
            @Param("minorCategory") String minorCategory,
            @Param("testType") com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.TestType testType,
            @Param("confidence") com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.Confidence confidence,
            @Param("keyword") String keyword);
}
