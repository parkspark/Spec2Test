package com.example.gameqacopilot.analysis.loop;

import java.time.LocalDateTime;

public record LatsLoopLogResponse(
        Long id,
        int depthStep,
        String generatedDraft,
        int evaluationScore,
        String evaluationFeedback,
        LocalDateTime createdAt) {

    static LatsLoopLogResponse from(LatsLoopLog log) {
        return new LatsLoopLogResponse(log.getId(), log.getDepthStep(), log.getGeneratedDraft(),
                log.getEvaluationScore(), log.getEvaluationFeedback(), log.getCreatedAt());
    }
}
