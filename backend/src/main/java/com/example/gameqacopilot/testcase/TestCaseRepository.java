package com.example.gameqacopilot.testcase;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findAllByAnalysisJob_Id(Long analysisJobId);
}
