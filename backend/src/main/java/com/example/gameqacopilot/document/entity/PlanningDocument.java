package com.example.gameqacopilot.document.entity;

import com.example.gameqacopilot.project.Project;
import com.example.gameqacopilot.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "planning_documents")
public class PlanningDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    private String title;
    private String originalFileName;
    private String storedFilePath;
    private String mimeType;
    private long fileSize;
    private Integer pageCount;
    @Column(columnDefinition = "TEXT") private String extractedText;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String pageContents;
    @Enumerated(EnumType.STRING) private DocumentProcessingStatus processingStatus;
    @Column(columnDefinition = "TEXT") private String failureReason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected PlanningDocument() {}

    public PlanningDocument(Project project, User createdBy, String title, String originalFileName, String mimeType, long fileSize) {
        this.project = project;
        this.createdBy = createdBy;
        this.title = title;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.processingStatus = DocumentProcessingStatus.UPLOADED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public void uploaded(String storedFilePath, int pageCount) {
        this.storedFilePath = storedFilePath;
        this.pageCount = pageCount;
        this.processingStatus = DocumentProcessingStatus.UPLOADED;
        this.updatedAt = LocalDateTime.now();
    }
    public void processed(String extractedText, String pageContents) {
        this.extractedText = extractedText;
        this.pageContents = pageContents;
        this.processingStatus = DocumentProcessingStatus.READY;
        this.updatedAt = LocalDateTime.now();
    }


    public void failed(String reason) {
        this.processingStatus = DocumentProcessingStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public Long getProjectId() { return project.getId(); }
    public String getTitle() { return title; }
    public String getOriginalFileName() { return originalFileName; }
    public String getStoredFilePath() { return storedFilePath; }
    public String getMimeType() { return mimeType; }
    public long getFileSize() { return fileSize; }
    public Integer getPageCount() { return pageCount; }
    public String getExtractedText() { return extractedText; }
    public String getPageContents() { return pageContents; }
    public DocumentProcessingStatus getProcessingStatus() { return processingStatus; }
    public String getFailureReason() { return failureReason; }
    public Long getCreatedById() { return createdBy.getId(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
