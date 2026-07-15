package com.example.gameqacopilot.output;

import com.example.gameqacopilot.ambiguity.Ambiguity;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.project.Project;
import com.example.gameqacopilot.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outputs")
public class Output {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planning_document_id", nullable = false)
    private PlanningDocument planningDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambiguity_id")
    private Ambiguity ambiguity;

    private String outputType;

    @Enumerated(EnumType.STRING)
    private OutputStatus status;

    @Column(columnDefinition = "TEXT")
    private String finalContent;

    private String fileName;
    private String externalService;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String requestData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String responseData;

    private String externalResourceId;
    private String externalUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String failureReason;

    protected Output() {}

    public Output(Project project, PlanningDocument planningDocument, User createdBy, String outputType, String fileName) {
        this.project = project;
        this.planningDocument = planningDocument;
        this.createdBy = createdBy;
        this.outputType = outputType;
        this.status = OutputStatus.PENDING;
        this.fileName = fileName;
        this.externalService = "NONE";
        this.requestData = "{\"projectId\":" + project.getId() + "}";
        this.createdAt = LocalDateTime.now();
    }

    public Output(Project project, PlanningDocument planningDocument, Ambiguity ambiguity,
            User createdBy, String requestData) {
        this.project = project;
        this.planningDocument = planningDocument;
        this.ambiguity = ambiguity;
        this.createdBy = createdBy;
        this.outputType = "JIRA_ISSUE";
        this.status = OutputStatus.PENDING;
        this.externalService = "JIRA";
        this.requestData = requestData;
        this.createdAt = LocalDateTime.now();
    }

    public void succeed(String content) {
        requirePending();
        status = OutputStatus.SUCCESS;
        finalContent = content;
        completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        requirePending();
        status = OutputStatus.FAILED;
        failureReason = reason;
        completedAt = LocalDateTime.now();
    }

    public void succeedJira(String content, String responseData, String issueKey, String issueUrl) {
        succeed(content);
        this.responseData = responseData;
        this.externalResourceId = issueKey;
        this.externalUrl = issueUrl;
    }

    private void requirePending() {
        if (status != OutputStatus.PENDING) {
            throw new IllegalStateException("Output is not pending");
        }
    }

    public Long getId() { return id; }
    public Long getProjectId() { return project.getId(); }
    public Long getPlanningDocumentId() { return planningDocument.getId(); }
    public Long getAmbiguityId() { return ambiguity == null ? null : ambiguity.getId(); }
    public String getOutputType() { return outputType; }
    public OutputStatus getStatus() { return status; }
    public String getFinalContent() { return finalContent; }
    public String getFileName() { return fileName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }
    public String getRequestData() { return requestData; }
    public String getResponseData() { return responseData; }
    public String getExternalResourceId() { return externalResourceId; }
    public String getExternalUrl() { return externalUrl; }
}
