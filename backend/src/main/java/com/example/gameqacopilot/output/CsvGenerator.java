package com.example.gameqacopilot.output;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.testcase.TestCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CsvGenerator {
    static final String HEADER = "No,대분류,중분류,소분류,테스트 항목,사전조건,테스트 스텝,기대결과,기획서 원문 근거,비고";
    private final ObjectMapper objectMapper;

    public CsvGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generate(List<TestCase> testCases) {
        StringBuilder csv = new StringBuilder("\uFEFF").append(HEADER).append("\r\n");
        for (TestCase testCase : testCases) {
            List<String> row = new ArrayList<>(10);
            row.add(String.valueOf(testCase.getDisplayOrder()));
            row.add(testCase.getMajorCategory());
            row.add(orDash(testCase.getMiddleCategory()));
            row.add(testCase.getMinorCategory());
            row.add(testCase.getTestItem());
            row.add(lines(read(testCase.getPreconditions(), new TypeReference<List<String>>() {})));
            row.add(steps(read(testCase.getTestSteps(), new TypeReference<List<AiAnalysisResponse.TestStep>>() {})));
            row.add(lines(read(testCase.getExpectedResults(), new TypeReference<List<String>>() {})));
            row.add(evidences(read(testCase.getEvidences(), new TypeReference<List<AiAnalysisResponse.Evidence>>() {})));
            row.add(lines(read(testCase.getNotes(), new TypeReference<List<String>>() {})));
            csv.append(row.stream().map(this::escape).reduce((a, b) -> a + "," + b).orElseThrow())
                    .append("\r\n");
        }
        return csv.toString();
    }

    private String steps(List<AiAnalysisResponse.TestStep> steps) {
        return steps == null || steps.isEmpty() ? "-" : String.join("\n", steps.stream()
                .map(step -> step.stepNumber() + ". " + step.action())
                .toList());
    }

    private String evidences(List<AiAnalysisResponse.Evidence> evidences) {
        return evidences == null || evidences.isEmpty() ? "-" : String.join("\n", evidences.stream()
                .map(evidence -> "[p." + evidence.pageNumber() + " / " + orDash(evidence.sectionTitle())
                        + " / " + evidence.evidenceType() + "]\n" + evidence.sourceText())
                .toList());
    }

    private String lines(List<String> values) {
        return values == null || values.isEmpty() ? "-" : String.join("\n", values);
    }

    private String escape(String value) {
        String safe = value == null ? "" : value;
        return safe.matches("(?s).*[\",\r\n].*") ? '"' + safe.replace("\"", "\"\"") + '"' : safe;
    }

    private String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored test case data is invalid", exception);
        }
    }
}
