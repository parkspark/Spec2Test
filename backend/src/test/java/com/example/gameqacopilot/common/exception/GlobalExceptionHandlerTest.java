package com.example.gameqacopilot.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsBadRequestForIllegalArgument() {
        var problem = handler.handleIllegalArgument(new IllegalArgumentException("invalid value"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("invalid value");
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        var problem = handler.handleUnexpected(new RuntimeException("secret"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).doesNotContain("secret");
    }
}
