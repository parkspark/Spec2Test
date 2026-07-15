package com.example.gameqacopilot.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.testcase.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvGeneratorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CsvGenerator generator = new CsvGenerator(objectMapper);

    @Test
    void writesBomFixedColumnsAndRfc4180Escaping() throws Exception {
        TestCase testCase = mock(TestCase.class);
        when(testCase.getDisplayOrder()).thenReturn(3);
        when(testCase.getMajorCategory()).thenReturn("상점,인벤토리");
        when(testCase.getMiddleCategory()).thenReturn("");
        when(testCase.getMinorCategory()).thenReturn("무료 뽑기");
        when(testCase.getTestItem()).thenReturn("\"무료\" 횟수 확인");
        when(testCase.getPreconditions()).thenReturn(json(List.of()));
        when(testCase.getTestSteps()).thenReturn(json(List.of(
                new AiAnalysisResponse.TestStep(1, "뽑기 화면에 진입한다.", "버튼이 표시된다."),
                new AiAnalysisResponse.TestStep(2, "버튼을 선택한다.", "결과가 표시된다."))));
        when(testCase.getExpectedResults()).thenReturn(json(List.of("횟수가 0으로 변경된다.")));
        when(testCase.getEvidences()).thenReturn(json(List.of(new AiAnalysisResponse.Evidence(
                AiAnalysisResponse.EvidenceType.EXPLICIT,
                AiAnalysisResponse.VerificationStatus.EXACT,
                7,
                "무료 뽑기 정책",
                null,
                "하루에 한 번 무료 뽑기를 할 수 있다.",
                AiAnalysisResponse.SourceElementType.TEXT,
                null,
                "횟수 근거"))));
        when(testCase.getNotes()).thenReturn(json(List.of("AI 추론 포함", "QA 확인 필요")));

        String csv = generator.generate(List.of(testCase));

        assertThat(csv).startsWith("\uFEFF" + CsvGenerator.HEADER + "\r\n");
        assertThat(csv).contains("3,\"상점,인벤토리\",-,무료 뽑기,\"\"\"무료\"\" 횟수 확인\",-");
        assertThat(csv).contains("\"1. 뽑기 화면에 진입한다.\n2. 버튼을 선택한다.\"");
        assertThat(csv).contains("\"[p.7 / 무료 뽑기 정책 / EXPLICIT]\n하루에 한 번 무료 뽑기를 할 수 있다.\"");
        assertThat(csv).endsWith("\"AI 추론 포함\nQA 확인 필요\"\r\n");
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
