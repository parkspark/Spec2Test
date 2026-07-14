package com.example.gameqacopilot.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void wrapsResponseData() {
        assertThat(ApiResponse.of("ok").data()).isEqualTo("ok");
    }
}
