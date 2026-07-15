package com.example.gameqacopilot.testcase;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.project.Project;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "test_cases")
public class TestCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "planning_document_id", nullable = false)
    private PlanningDocument planningDocument;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "analysis_job_id", nullable = false)
    private AnalysisJob analysisJob;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;
    private String externalTestCaseId;
    private int displayOrder;
    private String majorCategory;
    private String middleCategory;
    private String minorCategory;
    private String testItem;
    @Enumerated(EnumType.STRING) private AiAnalysisResponse.TestType testType;
    private String priority;
    @Enumerated(EnumType.STRING) private AiAnalysisResponse.Confidence confidence;
    @Enumerated(EnumType.STRING) private TestCaseStatus status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String preconditions;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String testSteps;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String expectedResults;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String evidences;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String notes;
    private boolean requiresHumanReview;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private User reviewedBy;
    private LocalDateTime reviewedAt;
    @Column(columnDefinition = "TEXT") private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected TestCase() {}

    public TestCase(AnalysisJob job, Requirement requirement, AiAnalysisResponse.TestCase value,
            String preconditions, String testSteps, String expectedResults, String evidences, String notes,
            boolean requiresHumanReview) {
        this.project = job.getPlanningDocument().getProject();
        this.planningDocument = job.getPlanningDocument();
        this.analysisJob = job;
        this.requirement = requirement;
        this.externalTestCaseId = value.testCaseId();
        this.displayOrder = value.displayOrder();
        this.majorCategory = value.majorCategory();
        this.middleCategory = value.middleCategory();
        this.minorCategory = value.minorCategory();
        this.testItem = value.testItem();
        this.testType = value.testType();
        this.priority = value.priority();
        this.confidence = value.confidence();
        this.status = TestCaseStatus.GENERATED;
        this.preconditions = preconditions;
        this.testSteps = testSteps;
        this.expectedResults = expectedResults;
        this.evidences = evidences;
        this.notes = notes;
        this.requiresHumanReview = requiresHumanReview;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public String getExternalTestCaseId() { return externalTestCaseId; }
    public Long getId() { return id; }
    public Long getAnalysisJobId() { return analysisJob.getId(); }
    public Long getRequirementId() { return requirement.getId(); }
    public int getDisplayOrder() { return displayOrder; }
    public String getMajorCategory() { return majorCategory; }
    public String getMiddleCategory() { return middleCategory; }
    public String getMinorCategory() { return minorCategory; }
    public String getTestItem() { return testItem; }
    public AiAnalysisResponse.TestType getTestType() { return testType; }
    public String getPriority() { return priority; }
    public AiAnalysisResponse.Confidence getConfidence() { return confidence; }
    public TestCaseStatus getStatus() { return status; }
    public String getPreconditions() { return preconditions; }
    public String getTestSteps() { return testSteps; }
    public String getExpectedResults() { return expectedResults; }
    public String getEvidences() { return evidences; }
    public String getNotes() { return notes; }
    public boolean isRequiresHumanReview() { return requiresHumanReview; }
    public Long getReviewedById() { return reviewedBy == null ? null : reviewedBy.getId(); }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public Integer getDocumentPageCount() { return planningDocument.getPageCount(); }
    public String getRequirementExternalId() { return requirement.getExternalRequirementId(); }
    public void approve(User reviewer) {
        review(TestCaseStatus.APPROVED, reviewer, null);
    }
    public void reject(User reviewer, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Rejection reason is required");
        review(TestCaseStatus.REJECTED, reviewer, reason.trim());
    }
    private void review(TestCaseStatus reviewedStatus, User reviewer, String reason) {
        if (status != TestCaseStatus.GENERATED) throw new IllegalArgumentException("Test case is already reviewed");
        status = reviewedStatus;
        reviewedBy = reviewer;
        reviewedAt = LocalDateTime.now();
        rejectionReason = reason;
        updatedAt = reviewedAt;
    }
    public void updateNotesForAmbiguity(String updatedNotes) {
        this.notes = updatedNotes;
        this.requiresHumanReview = true;
        this.updatedAt = LocalDateTime.now();
    }
}
