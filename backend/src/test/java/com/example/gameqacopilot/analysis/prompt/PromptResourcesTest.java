package com.example.gameqacopilot.analysis.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptResourcesTest {
    @Test
    void packagesVersionedPromptsWithRequiredRulesAndSchemaNames() throws Exception {
        var names = List.of("system", "classification", "requirement", "test-case", "ambiguity", "evidence", "prohibitions");
        var content = new StringBuilder();
        for (var name : names) {
            try (var input = getClass().getResourceAsStream("/prompts/v1.0/" + name + ".txt")) {
                assertThat(input).as(name).isNotNull();
                content.append(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertThat(content.toString()).contains("categoryTree", "displayOrder", "testSteps", "expectedResults");
        assertThat(content.toString()).contains("ambiguities", "relatedRequirementIds");
        assertThat(content.toString()).contains("EXPLICIT", "INFERRED", "UNSUPPORTED");
        assertThat(content.toString()).contains("EXACT", "PARTIAL", "SIMILAR", "NOT_FOUND");
    }
}
