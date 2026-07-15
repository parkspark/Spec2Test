package com.example.gameqacopilot.requirement;

import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "requirements")
public class Requirement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_job_id", nullable = false)
    private AnalysisJob analysisJob;
    private String externalRequirementId;
    private String majorCategory;
    private String middleCategory;
    private String minorCategory;
    private String title;
    @Column(columnDefinition = "TEXT") private String description;
    private String actor;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String preconditions;
    @Column(name = "requirement_trigger", columnDefinition = "TEXT") private String trigger;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String expectedBehaviors;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String constraints;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String evidences;
    private LocalDateTime createdAt;

    protected Requirement() {}

    public Requirement(AnalysisJob analysisJob, String externalRequirementId,
            String majorCategory, String middleCategory, String minorCategory,
            String title, String description, String actor, String preconditions,
            String trigger, String expectedBehaviors, String constraints, String evidences) {
        this.analysisJob = analysisJob;
        this.externalRequirementId = externalRequirementId;
        this.majorCategory = majorCategory;
        this.middleCategory = middleCategory;
        this.minorCategory = minorCategory;
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.preconditions = preconditions;
        this.trigger = trigger;
        this.expectedBehaviors = expectedBehaviors;
        this.constraints = constraints;
        this.evidences = evidences;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getExternalRequirementId() { return externalRequirementId; }
    public String getMajorCategory() { return majorCategory; }
    public String getMiddleCategory() { return middleCategory; }
    public String getMinorCategory() { return minorCategory; }
    public String getEvidences() { return evidences; }
}
