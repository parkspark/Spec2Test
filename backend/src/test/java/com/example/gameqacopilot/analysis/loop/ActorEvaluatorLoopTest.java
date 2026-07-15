package com.example.gameqacopilot.analysis.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ActorEvaluatorLoopTest {
    private final LoopPersistenceService persistence = mock(LoopPersistenceService.class);
    private final ActorEvaluatorLoop loop = new ActorEvaluatorLoop(persistence);

    @Test
    void regeneratesWithFeedbackUntilPassingAndRecordsEveryRound() {
        var feedbacks = new ArrayList<String>();
        var scores = List.of(60, 85);
        var evaluation = new AtomicInteger();

        loop.run(9L, 80,
                (round, feedback) -> {
                    feedbacks.add(feedback);
                    return "draft-" + round;
                },
                draft -> new Evaluation(scores.get(evaluation.getAndIncrement()), "fix-" + draft));

        assertThat(feedbacks).containsExactly(null, "fix-draft-1");
        verify(persistence).record(9L, 1, "draft-1", new Evaluation(60, "fix-draft-1"));
        verify(persistence).record(9L, 2, "draft-2", new Evaluation(85, "fix-draft-2"));
        verify(persistence).succeed(9L, "draft-2");
        verify(persistence, never()).fail(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void failsAfterThreeRejectedRounds() {
        loop.run(9L, 80, (round, feedback) -> "draft-" + round, draft -> new Evaluation(79, "revise"));

        verify(persistence).record(9L, 1, "draft-1", new Evaluation(79, "revise"));
        verify(persistence).record(9L, 2, "draft-2", new Evaluation(79, "revise"));
        verify(persistence).record(9L, 3, "draft-3", new Evaluation(79, "revise"));
        verify(persistence).fail(9L, "Evaluator score did not reach 80 within 3 rounds");
        verify(persistence, never()).succeed(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }
}
