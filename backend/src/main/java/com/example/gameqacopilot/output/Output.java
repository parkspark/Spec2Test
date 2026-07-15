package com.example.gameqacopilot.output;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String failureReason;

    protected Output() {}

    public Output(Project project, PlanningDocument planningDocument, User createdBy, String fileName) {
        this.project = project;
        this.planningDocument = planningDocument;
        this.createdBy = createdBy;
        this.outputType = "CSV_EXPORT";
        this.status = OutputStatus.PENDING;
        this.fileName = fileName;
        this.externalService = "NONE";
        this.requestData = "{\"projectId\":" + project.getId() + "}";
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

    private void requirePending() {
        if (status != OutputStatus.PENDING) {
            throw new IllegalStateException("Output is not pending");
        }
    }

    public Long getId() { return id; }
    public Long getProjectId() { return project.getId(); }
    public Long getPlanningDocumentId() { return planningDocument.getId(); }
    public String getOutputType() { return outputType; }
    public OutputStatus getStatus() { return status; }
    public String getFinalContent() { return finalContent; }
    public String getFileName() { return fileName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }
}
