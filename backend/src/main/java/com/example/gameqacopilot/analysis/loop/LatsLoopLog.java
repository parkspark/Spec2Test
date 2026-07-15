package com.example.gameqacopilot.analysis.loop;

import com.example.gameqacopilot.output.Output;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "lats_loop_logs")
public class LatsLoopLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_id", nullable = false)
    private Output output;

    private int depthStep;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String generatedDraft;

    private int evaluationScore;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String evaluationFeedback;

    private LocalDateTime createdAt;

    protected LatsLoopLog() {}

    public LatsLoopLog(Output output, int depthStep, String draft, int score, String feedback) {
        this.output = output;
        this.depthStep = depthStep;
        this.generatedDraft = draft;
        this.evaluationScore = score;
        this.evaluationFeedback = feedback;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public int getDepthStep() { return depthStep; }
    public String getGeneratedDraft() { return generatedDraft; }
    public int getEvaluationScore() { return evaluationScore; }
    public String getEvaluationFeedback() { return evaluationFeedback; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
