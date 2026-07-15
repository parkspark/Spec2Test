package com.example.gameqacopilot.analysis.loop;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class ActorEvaluatorLoop {
    static final int MAX_ROUNDS = 3;
    private final LoopPersistenceService persistence;

    public ActorEvaluatorLoop(LoopPersistenceService persistence) {
        this.persistence = persistence;
    }

    public void run(
            Long outputId,
            int passScore,
            BiFunction<Integer, String, String> actor,
            Function<String, Evaluation> evaluator) {
        if (passScore < 0 || passScore > 100) {
            throw new IllegalArgumentException("Pass score must be between 0 and 100");
        }

        String feedback = null;
        for (int round = 1; round <= MAX_ROUNDS; round++) {
            String draft = actor.apply(round, feedback);
            Evaluation evaluation = evaluator.apply(draft);
            persistence.record(outputId, round, draft, evaluation);
            if (evaluation.score() >= passScore) {
                persistence.succeed(outputId, draft);
                return;
            }
            feedback = evaluation.feedback();
        }
        persistence.fail(outputId, "Evaluator score did not reach " + passScore + " within " + MAX_ROUNDS + " rounds");
    }
}
