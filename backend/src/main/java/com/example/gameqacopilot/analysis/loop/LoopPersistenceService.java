package com.example.gameqacopilot.analysis.loop;

import com.example.gameqacopilot.output.Output;
import com.example.gameqacopilot.output.OutputRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoopPersistenceService {
    private final OutputRepository outputs;
    private final LatsLoopLogRepository logs;

    public LoopPersistenceService(OutputRepository outputs, LatsLoopLogRepository logs) {
        this.outputs = outputs;
        this.logs = logs;
    }

    @Transactional
    public void record(Long outputId, int round, String draft, Evaluation evaluation) {
        Output output = requireOutput(outputId);
        logs.save(new LatsLoopLog(output, round, draft, evaluation.score(), evaluation.feedback()));
    }

    @Transactional
    public void succeed(Long outputId, String content) {
        requireOutput(outputId).succeed(content);
    }

    @Transactional
    public void fail(Long outputId, String reason) {
        requireOutput(outputId).fail(reason);
    }

    private Output requireOutput(Long outputId) {
        return outputs.findById(outputId)
                .orElseThrow(() -> new NoSuchElementException("Output not found"));
    }
}
