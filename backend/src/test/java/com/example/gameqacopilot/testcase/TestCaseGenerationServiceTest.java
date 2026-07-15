package com.example.gameqacopilot.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import com.example.gameqacopilot.analysis.service.AnalysisJobService;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.project.Project;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ByteArrayResource;

class TestCaseGenerationServiceTest {
    private final AnalysisJobService jobs = mock(AnalysisJobService.class);
    private final RequirementRepository requirements = mock(RequirementRepository.class);
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final ChatModel chatModel = mock(ChatModel.class);

    @Test
    void generatesTenColumnTestCasesAndSavesJsonEvidence() throws Exception {
        var job = mock(AnalysisJob.class);
        var document = mock(PlanningDocument.class);
        when(job.getPlanningDocument()).thenReturn(document);
        when(document.getProject()).thenReturn(mock(Project.class));
        when(jobs.prepareRequirements(5L)).thenReturn(
                new AnalysisJobService.RequirementInput(job, mock(AnalysisJobService.AnalysisInput.class)));
        var requirement = mock(Requirement.class);
        when(requirement.getExternalRequirementId()).thenReturn("REQ-001");
        when(requirement.getMajorCategory()).thenReturn("무료 뽑기");
        when(requirement.getMiddleCategory()).thenReturn("보상 지급");
        when(requirement.getMinorCategory()).thenReturn("일일 무료 횟수");
        when(requirements.findAllByAnalysisJob_Id(5L)).thenReturn(List.of(requirement));
        String json = new ObjectMapper().writeValueAsString(
                new com.example.gameqacopilot.analysis.dto.TestCaseGenerationResponse(List.of(value())));
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage(json)))));
        var categories = List.of(new AiAnalysisResponse.CategoryTree("무료 뽑기",
                List.of(new AiAnalysisResponse.MiddleCategory("보상 지급", List.of("일일 무료 횟수")))));

        var response = service().generate(5L, categories, List.of());

        assertThat(response.testCases()).hasSize(1);
        var saved = ArgumentCaptor.forClass(Iterable.class);
        verify(testCases).saveAll(saved.capture());
        assertThat(saved.getValue()).asList().singleElement().satisfies(value -> {
            var testCase = (TestCase) value;
            assertThat(testCase.getStatus()).isEqualTo(TestCaseStatus.GENERATED);
            assertThat(testCase.getEvidences()).contains("sourceText");
        });
        verify(jobs).recordTestCases(eq(5L), anyString(), eq(0L));
    }

    private TestCaseGenerationService service() {
        return new TestCaseGenerationService(jobs, requirements, testCases,
                ChatClient.builder(chatModel), new ObjectMapper(),
                new ByteArrayResource("system".getBytes()),
                new ByteArrayResource("test case".getBytes()));
    }

    private AiAnalysisResponse.TestCase value() {
        var evidence = new AiAnalysisResponse.Evidence(
                AiAnalysisResponse.EvidenceType.EXPLICIT, AiAnalysisResponse.VerificationStatus.EXACT,
                1, "정책", "PAGE-1-TEXT-1", "하루 한 번",
                AiAnalysisResponse.SourceElementType.TEXT, null, "직접 명시");
        var step = new AiAnalysisResponse.TestStep(1, "버튼을 선택한다.", "결과가 표시된다.");
        return new AiAnalysisResponse.TestCase(
                "TC-001", "REQ-001", 1, "무료 뽑기", "보상 지급", "일일 무료 횟수",
                "무료 뽑기 정상 사용", AiAnalysisResponse.TestType.HAPPY_PATH, "HIGH",
                List.of("로그인 상태"), List.of(step), List.of("보상이 한 번 지급된다."),
                AiAnalysisResponse.Confidence.HIGH, false, List.of(evidence), List.of());
    }
}
