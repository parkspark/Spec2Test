package com.example.gameqacopilot.analysis.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AnalysisResultValidatorTest {
    @Test
    void retriesOnceAfterInvalidResponse() {
        var calls = new AtomicInteger();

        String result = AnalysisResultValidator.validateWithOneRetry(
                () -> calls.incrementAndGet() == 1 ? "invalid" : "valid",
                value -> {
                    if (!value.equals("valid")) throw new IllegalArgumentException("invalid schema");
                });

        assertThat(result).isEqualTo("valid");
        assertThat(calls).hasValue(2);
    }

    @Test
    void stopsAfterOneRetryAndKeepsLastFailure() {
        var calls = new AtomicInteger();

        assertThatThrownBy(() -> AnalysisResultValidator.validateWithOneRetry(
                () -> {
                    calls.incrementAndGet();
                    return "invalid";
                },
                value -> { throw new IllegalArgumentException("invalid result"); }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid result");
        assertThat(calls).hasValue(2);
    }
}
