package com.example.gameqacopilot.ambiguity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.analysis.dto.AmbiguityGenerationResponse;
import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import com.example.gameqacopilot.analysis.service.AnalysisJobService;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.example.gameqacopilot.testcase.TestCase;
import com.example.gameqacopilot.testcase.TestCaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ByteArrayResource;

class AmbiguityGenerationServiceTest {
    private final AnalysisJobService jobs = mock(AnalysisJobService.class);
    private final RequirementRepository requirements = mock(RequirementRepository.class);
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final AmbiguityRepository ambiguities = mock(AmbiguityRepository.class);
    private final ChatModel chatModel = mock(ChatModel.class);

    @Test
    void savesQuestionsAndLinksRelatedTestCaseNotes() throws Exception {
        var job = mock(AnalysisJob.class);
        when(job.getPlanningDocument()).thenReturn(mock(PlanningDocument.class));
        when(jobs.prepareRequirements(6L)).thenReturn(
                new AnalysisJobService.RequirementInput(job, mock(AnalysisJobService.AnalysisInput.class)));
        var requirement = mock(Requirement.class);
        when(requirement.getExternalRequirementId()).thenReturn(\u0022REQ-001\u0022);
        when(requirement.getMajorCategory()).thenReturn(\u0022무료 뽑기\u0022);
        when(requirement.getMiddleCategory()).thenReturn(\u0022초기화\u0022);
        when(requirement.getMinorCategory()).thenReturn(\u0022초기화 시간\u0022);
        when(requirements.findAllByAnalysisJob_Id(6L)).thenReturn(List.of(requirement));
        var testCase = mock(TestCase.class);
        when(testCase.getRequirementExternalId()).thenReturn(\u0022REQ-001\u0022);
        when(testCase.getNotes()).thenReturn(\u0022[]\u0022);
        when(testCases.findAllByAnalysisJob_Id(6L)).thenReturn(List.of(testCase));
        var response = new AmbiguityGenerationResponse(List.of(value()));
        String json = new ObjectMapper().writeValueAsString(response);
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage(json)))));

        var result = service().generate(6L, List.of(), List.of());

        assertThat(result.ambiguities()).singleElement().satisfies(value ->
                assertThat(value.question()).contains(\u0022서버 시간\u0022));
        var saved = ArgumentCaptor.forClass(Iterable.class);
        verify(ambiguities).saveAll(saved.capture());
        assertThat(saved.getValue()).asList().singleElement().satisfies(value -> {
            var ambiguity = (Ambiguity) value;
            assertThat(ambiguity.getStatus()).isEqualTo(AmbiguityStatus.OPEN);
            assertThat(ambiguity.getEvidences()).contains(\u0022sourceText\u0022);
        });
        var notes = ArgumentCaptor.forClass(String.class);
        verify(testCase).updateNotesForAmbiguity(notes.capture());
        assertThat(notes.getValue()).contains(\u0022관련 모호성: AMB-001\u0022, \u0022기획 확인 필요\u0022);
        verify(jobs).recordAmbiguities(eq(6L), anyString(), eq(0L));
    }
    private AmbiguityGenerationService service() {
        return new AmbiguityGenerationService(jobs, requirements, testCases, ambiguities,
                ChatClient.builder(chatModel), new ObjectMapper(),
                new ByteArrayResource(\u0022system\u0022.getBytes()),
                new ByteArrayResource(\u0022ambiguity\u0022.getBytes()));
    }

    private AiAnalysisResponse.Ambiguity value() {
        var evidence = new AiAnalysisResponse.Evidence(
                AiAnalysisResponse.EvidenceType.INFERRED, null, 3, \u0022무료 뽑기 정책\u0022, null,
                \u0022무료 뽑기는 매일 초기화된다.\u0022, AiAnalysisResponse.SourceElementType.TEXT,
                null, \u0022기준 시간이 정의되지 않았다.\u0022);
        return new AiAnalysisResponse.Ambiguity(
                \u0022AMB-001\u0022, List.of(\u0022REQ-001\u0022), \u0022무료 뽑기\u0022,
                \u0022초기화\u0022, \u0022초기화 시간\u0022, \u0022초기화 시간 기준 불명확\u0022,
                \u0022정확한 시간 기준이 없다.\u0022, \u0022서버 시간 기준으로 초기화되나요?\u0022,
                \u0022경계 테스트를 확정할 수 없다.\u0022, \u0022HIGH\u0022, List.of(evidence));
    }
}
