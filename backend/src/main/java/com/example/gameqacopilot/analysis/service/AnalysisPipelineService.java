package com.example.gameqacopilot.analysis.service;

import com.example.gameqacopilot.ambiguity.AmbiguityGenerationService;
import com.example.gameqacopilot.analysis.dto.AnalysisJobResponse;
import com.example.gameqacopilot.requirement.RequirementExtractionService;
import com.example.gameqacopilot.testcase.TestCaseGenerationService;
import org.springframework.stereotype.Service;

@Service
public class AnalysisPipelineService {
    private final AnalysisJobService jobs;
    private final CategoryClassificationService classifications;
    private final RequirementExtractionService requirements;
    private final TestCaseGenerationService testCases;
    private final AmbiguityGenerationService ambiguities;

    public AnalysisPipelineService(AnalysisJobService jobs, CategoryClassificationService classifications,
            RequirementExtractionService requirements, TestCaseGenerationService testCases,
            AmbiguityGenerationService ambiguities) {
        this.jobs = jobs;
        this.classifications = classifications;
        this.requirements = requirements;
        this.testCases = testCases;
        this.ambiguities = ambiguities;
    }

    public AnalysisJobResponse run(Long documentId, Long userId) {
        var job = jobs.request(documentId, userId);
        var classification = classifications.classify(job.id());
        var requirement = requirements.extract(job.id(), classification.categoryTree());
        var testCase = testCases.generate(job.id(), classification.categoryTree(), requirement.requirements());
        ambiguities.generate(job.id(), requirement.requirements(), testCase.testCases());
        jobs.complete(job.id());
        return jobs.findById(job.id());
    }
}
