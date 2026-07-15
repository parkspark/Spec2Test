package com.example.gameqacopilot.output;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "outputs")
public class Output {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OutputStatus status;

    @Column(columnDefinition = "TEXT")
    private String finalContent;

    private LocalDateTime completedAt;
    private String failureReason;

    protected Output() {}

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
    public OutputStatus getStatus() { return status; }
    public String getFinalContent() { return finalContent; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }
}
