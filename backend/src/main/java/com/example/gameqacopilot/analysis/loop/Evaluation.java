package com.example.gameqacopilot.analysis.loop;

public record Evaluation(int score, String feedback) {
    public Evaluation {
        if (score < 0 || score > 100) throw new IllegalArgumentException("Evaluation score must be between 0 and 100");
        if (feedback == null) throw new IllegalArgumentException("Evaluation feedback is required");
    }
}
