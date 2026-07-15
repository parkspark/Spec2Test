package com.example.gameqacopilot.requirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import com.example.gameqacopilot.analysis.service.AnalysisJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ByteArrayResource;

class RequirementExtractionServiceTest {
    private final AnalysisJobService jobs = mock(AnalysisJobService.class);
    private final RequirementRepository requirements = mock(RequirementRepository.class);
    private final ChatModel chatModel = mock(ChatModel.class);

    @Test
    void reusesCategoriesAndSavesRequirementsWithEvidenceTypes(@TempDir Path directory) throws Exception {
        Path pdf = Files.createFile(directory.resolve("original.pdf"));
        Files.createFile(directory.resolve("page-1.png"));
        var job = mock(AnalysisJob.class);
        when(jobs.prepareRequirements(4L)).thenReturn(new AnalysisJobService.RequirementInput(job,
                new AnalysisJobService.AnalysisInput("무료 뽑기", pageContents("무료 뽑기"), pdf.toString(), 1)));
        String json = responseJson();
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage(json)))));
        var categories = List.of(new AiAnalysisResponse.CategoryTree("무료 뽑기",
                List.of(new AiAnalysisResponse.MiddleCategory("보상 지급", List.of("일일 무료 횟수")))));

        var response = service().extract(4L, categories);

        assertThat(response.requirements()).extracting(AiAnalysisResponse.Requirement::requirementId)
                .containsExactly("REQ-001", "REQ-002", "REQ-003");
        var saved = ArgumentCaptor.forClass(Iterable.class);
        verify(requirements).saveAll(saved.capture());
        assertThat(saved.getValue()).asList().hasSize(3);
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getUserMessage().getText())
                .contains("무료 뽑기", "보상 지급", "일일 무료 횟수");
        assertThat(prompt.getValue().getUserMessage().getMedia()).hasSize(1);
        verify(jobs).recordRequirements(eq(4L), argThat(raw -> raw.strip().equals(json.strip())), eq(0L));
        verify(jobs, never()).fail(anyLong(), anyString());
    }

    private RequirementExtractionService service() {
        return new RequirementExtractionService(jobs, requirements, ChatClient.builder(chatModel),
                new ObjectMapper(), new ByteArrayResource("system".getBytes()),
                new ByteArrayResource("requirement".getBytes()));
    }

    private String pageContents(String text) {
        return "[{\"pageNumber\":1,\"elements\":[{\"text\":\"" + text + "\"}]}]";
    }

    private String responseJson() {
        return """
                {"requirements":[
                  %s,%s,%s
                ]}
                """.formatted(requirement("REQ-001", "EXPLICIT"),
                        requirement("REQ-002", "INFERRED"), requirement("REQ-003", "UNSUPPORTED"));
    }

    private String requirement(String id, String evidenceType) {
        return """
                {"requirementId":"%s","majorCategory":"무료 뽑기","middleCategory":"보상 지급",
                "minorCategory":"일일 무료 횟수","title":"제목","description":"설명","actor":"PLAYER",
                "preconditions":["로그인"],"trigger":"선택","expectedBehaviors":["지급"],
                "evidences":[{"evidenceType":"%s","pageNumber":1,"sectionTitle":"정책",
                "sourceText":"무료 뽑기","sourceElementType":"TEXT","reason":"근거"}]}
                """.formatted(id, evidenceType).strip();
    }
}
