package com.example.gameqacopilot.analysis.dto;

import java.util.List;

public record AiAnalysisResponse(
        DocumentSummary documentSummary,
        List<CategoryTree> categoryTree,
        List<Requirement> requirements,
        List<TestCase> testCases,
        List<Ambiguity> ambiguities,
        List<String> qualityWarnings) {
    public record DocumentSummary(String title, String featureSummary, List<String> actors) {}
    public record CategoryTree(String majorCategory, List<MiddleCategory> middleCategories) {}
    public record MiddleCategory(String name, List<String> minorCategories) {}
    public record Requirement(
            String requirementId, String majorCategory, String middleCategory, String minorCategory,
            String title, String description, String actor, List<String> preconditions, String trigger,
            List<String> expectedBehaviors, List<Evidence> evidences) {}
    public record TestCase(
            String testCaseId, String requirementId, int displayOrder,
            String majorCategory, String middleCategory, String minorCategory, String testItem,
            TestType testType, String priority, List<String> preconditions, List<TestStep> testSteps,
            List<String> expectedResults, Confidence confidence, boolean requiresHumanReview,
            List<Evidence> evidences, List<String> notes) {}
    public record TestStep(int stepNumber, String action, String expectedResult) {}
    public record Ambiguity(
            String ambiguityId, List<String> relatedRequirementIds,
            String majorCategory, String middleCategory, String minorCategory,
            String title, String description, String question, String impact,
            String severity, List<Evidence> evidences) {}
    public record Evidence(
            EvidenceType evidenceType, VerificationStatus verificationStatus, Integer pageNumber,
            String sectionTitle, String sourceElementId, String sourceText,
            SourceElementType sourceElementType, BoundingBox boundingBox, String reason) {}
    public record BoundingBox(double x, double y, double width, double height) {}
    public enum EvidenceType { EXPLICIT, INFERRED, UNSUPPORTED }
    public enum VerificationStatus { EXACT, PARTIAL, SIMILAR, NOT_FOUND }
    public enum SourceElementType { TEXT, TABLE, IMAGE, CAPTION }
    public enum TestType {
        HAPPY_PATH, BOUNDARY, EXCEPTION, VALIDATION, STATE_TRANSITION, TIME, DUPLICATION, DATA_PERSISTENCE
    }
    public enum Confidence { HIGH, MEDIUM, LOW }
}
