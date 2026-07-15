package com.example.gameqacopilot.testcase;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import java.util.List;

public record TestCaseResponse(
        Long id,
        String externalTestCaseId,
        Long analysisId,
        Long requirementId,
        int displayOrder,
        String majorCategory,
        String middleCategory,
        String minorCategory,
        String testItem,
        AiAnalysisResponse.TestType testType,
        String priority,
        AiAnalysisResponse.Confidence confidence,
        TestCaseStatus status,
        List<String> preconditions,
        List<AiAnalysisResponse.TestStep> testSteps,
        List<String> expectedResults,
        EvidenceSummary evidenceSummary,
        List<AiAnalysisResponse.Evidence> evidences,
        List<String> notes,
        boolean requiresHumanReview) {

    public record EvidenceSummary(
            Integer pageNumber,
            AiAnalysisResponse.EvidenceType evidenceType,
            String sourceText) {
        static EvidenceSummary from(AiAnalysisResponse.Evidence evidence) {
            return evidence == null ? null
                    : new EvidenceSummary(evidence.pageNumber(), evidence.evidenceType(), evidence.sourceText());
        }
    }
}
