package com.example.gameqacopilot.analysis.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.output.Output;
import com.example.gameqacopilot.output.OutputRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LatsLoopLogQueryServiceTest {
    @Test
    void returnsLoopLogsInRoundOrder() {
        OutputRepository outputs = mock(OutputRepository.class);
        LatsLoopLogRepository logs = mock(LatsLoopLogRepository.class);
        LatsLoopLog log = new LatsLoopLog(mock(Output.class), 1, "draft", 95, "good");
        ReflectionTestUtils.setField(log, "id", 3L);
        when(outputs.existsById(9L)).thenReturn(true);
        when(logs.findAllByOutput_IdOrderByDepthStep(9L)).thenReturn(List.of(log));

        List<LatsLoopLogResponse> result = new LatsLoopLogQueryService(outputs, logs).findByOutputId(9L);

        assertThat(result).containsExactly(new LatsLoopLogResponse(3L, 1, "draft", 95, "good", log.getCreatedAt()));
    }
}
