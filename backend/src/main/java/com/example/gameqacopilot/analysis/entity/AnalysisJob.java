package com.example.gameqacopilot.analysis.entity;

import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_jobs")
public class AnalysisJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planning_document_id", nullable = false)
    private PlanningDocument planningDocument;
    @Enumerated(EnumType.STRING)
    private AnalysisJobStatus status;
    private String modelName;
    private String promptVersion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    @Column(columnDefinition = "TEXT") private String failureReason;
    private Long tokenUsage;
    @Column(columnDefinition = "TEXT") private String rawResponse;
    private LocalDateTime createdAt;

    protected AnalysisJob() {}

    public AnalysisJob(PlanningDocument planningDocument, User requestedBy) {
        this.planningDocument = planningDocument;
        this.requestedBy = requestedBy;
        this.status = AnalysisJobStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
        this.createdAt = requestedAt;
    }

    public void start(String modelName, String promptVersion) {
        this.status = AnalysisJobStatus.PROCESSING;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.startedAt = LocalDateTime.now();
    }

    public void recordClassification(String rawResponse, Long tokenUsage) {
        this.rawResponse = rawResponse;
        this.tokenUsage = tokenUsage;
    }

    public void fail(String reason) {
        this.status = AnalysisJobStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getPlanningDocumentId() { return planningDocument.getId(); }
    public AnalysisJobStatus getStatus() { return status; }
    public String getModelName() { return modelName; }
    public String getPromptVersion() { return promptVersion; }
    public Long getRequestedById() { return requestedBy.getId(); }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }
    public Long getTokenUsage() { return tokenUsage; }
    public String getRawResponse() { return rawResponse; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
