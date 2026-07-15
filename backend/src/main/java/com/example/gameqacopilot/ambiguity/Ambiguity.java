package com.example.gameqacopilot.ambiguity;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = \u0022ambiguities\u0022)
public class Ambiguity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = \u0022planning_document_id\u0022, nullable = false)
    private PlanningDocument planningDocument;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = \u0022analysis_job_id\u0022, nullable = false)
    private AnalysisJob analysisJob;
    private String externalAmbiguityId;
    private String majorCategory;
    private String middleCategory;
    private String minorCategory;
    private String title;
    @Column(columnDefinition = \u0022TEXT\u0022) private String description;
    @Column(columnDefinition = \u0022TEXT\u0022) private String question;
    @Column(columnDefinition = \u0022TEXT\u0022) private String impact;
    private String severity;
    @Enumerated(EnumType.STRING) private AmbiguityStatus status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = \u0022jsonb\u0022) private String evidences;
    private String jiraIssueKey;
    private String jiraIssueUrl;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    protected Ambiguity() {}

    public Ambiguity(AnalysisJob job, AiAnalysisResponse.Ambiguity value, String evidences) {
        this.planningDocument = job.getPlanningDocument();
        this.analysisJob = job;
        this.externalAmbiguityId = value.ambiguityId();
        this.majorCategory = value.majorCategory();
        this.middleCategory = value.middleCategory();
        this.minorCategory = value.minorCategory();
        this.title = value.title();
        this.description = value.description();
        this.question = value.question();
        this.impact = value.impact();
        this.severity = value.severity();
        this.status = AmbiguityStatus.OPEN;
        this.evidences = evidences;
        this.createdAt = LocalDateTime.now();
    }

    public String getExternalAmbiguityId() { return externalAmbiguityId; }
    public String getQuestion() { return question; }
    public String getSeverity() { return severity; }
    public AmbiguityStatus getStatus() { return status; }
    public String getEvidences() { return evidences; }
}
