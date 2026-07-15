package com.example.gameqacopilot.analysis.loop;

import com.example.gameqacopilot.output.OutputRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LatsLoopLogQueryService {
    private final OutputRepository outputs;
    private final LatsLoopLogRepository logs;

    public LatsLoopLogQueryService(OutputRepository outputs, LatsLoopLogRepository logs) {
        this.outputs = outputs;
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public List<LatsLoopLogResponse> findByOutputId(Long outputId) {
        if (!outputs.existsById(outputId)) throw new NoSuchElementException("Output not found");
        return logs.findAllByOutput_IdOrderByDepthStep(outputId).stream()
                .map(LatsLoopLogResponse::from)
                .toList();
    }
}
