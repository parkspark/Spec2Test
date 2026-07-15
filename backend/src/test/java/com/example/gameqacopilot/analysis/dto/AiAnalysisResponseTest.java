package com.example.gameqacopilot.analysis.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AiAnalysisResponseTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesCompleteResponse() throws Exception {
        var response = objectMapper.readValue(validJson(), AiAnalysisResponse.class);
        assertThat(response.categoryTree()).hasSize(1);
        assertThat(response.requirements().getFirst().evidences()).hasSize(1);
        assertThat(response.testCases().getFirst().evidences()).hasSize(1);
        assertThat(response.ambiguities().getFirst().evidences()).hasSize(1);
    }

    @Test
    void rejectsMalformedJsonAndUnknownEnum() {
        assertThatThrownBy(() -> objectMapper.readValue("{", AiAnalysisResponse.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> objectMapper.readValue(
                        validJson().replace("HAPPY_PATH", "SMOKE"), AiAnalysisResponse.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void jsonSchemaIsPackagedAndValidJson() throws Exception {
        try (InputStream schema = getClass().getResourceAsStream("/schemas/ai-analysis-response.schema.json")) {
            assertThat(schema).isNotNull();
            var root = objectMapper.readTree(schema);
            assertThat(root.path("$defs").path("evidence").path("properties")
                    .path("verificationStatus").isObject()).isTrue();
            assertThat(root.path("$defs").path("testCase").path("required")).isNotEmpty();
        }
    }

    private String validJson() throws Exception {
        try (InputStream json = getClass().getResourceAsStream("/fixtures/ai-analysis-response.json")) {
            return new String(json.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
