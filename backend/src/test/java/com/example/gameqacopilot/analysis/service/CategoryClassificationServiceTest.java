package com.example.gameqacopilot.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

class CategoryClassificationServiceTest {
    private final AnalysisJobService jobs = mock(AnalysisJobService.class);
    private final ChatModel chatModel = mock(ChatModel.class);

    @Test
    void sendsExtractedTextPageDataAndEveryPageImage(@TempDir Path directory) throws Exception {
        Path pdf = Files.createFile(directory.resolve("original.pdf"));
        Files.createFile(directory.resolve("page-1.png"));
        Files.createFile(directory.resolve("page-2.png"));
        String pageData = """
                [{"pageNumber":1}]
                """.strip();
        when(jobs.startClassification(9L, "vision-model", "v1.0"))
                .thenReturn(new AnalysisJobService.AnalysisInput(
                        "훈련 시작 버튼", pageData, pdf.toString(), 2));
        String json = """
                {"categoryTree":[{"majorCategory":"궁술 훈련","middleCategories":[
                {"name":"-","minorCategories":["훈련 시작"]}]}],"evidences":[]}
                """;
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage(json)))));
        var service = service();

        var response = service.classify(9L);

        assertThat(response.categoryTree()).singleElement()
                .satisfies(category -> assertThat(category.majorCategory()).isEqualTo("궁술 훈련"));
        var prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getUserMessage().getText())
                .contains("훈련 시작 버튼", pageData);
        assertThat(prompt.getValue().getUserMessage().getMedia()).hasSize(2);
        verify(jobs).recordClassification(eq(9L), argThat(raw -> raw.strip().equals(json.strip())), eq(0L));
        verify(jobs, never()).fail(anyLong(), anyString());
    }

    @Test
    void rejectsMissingDashCategoryAndRecordsFailure(@TempDir Path directory) throws Exception {
        Path pdf = Files.createFile(directory.resolve("original.pdf"));
        Files.createFile(directory.resolve("page-1.png"));
        when(jobs.startClassification(9L, "vision-model", "v1.0"))
                .thenReturn(new AnalysisJobService.AnalysisInput("text", "[]", pdf.toString(), 1));
        String json = """
                {"categoryTree":[{"majorCategory":"상점","middleCategories":[]}],"evidences":[]}
                """.strip();
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage(json)))));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().classify(9L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Feature classification failed");
        verify(chatModel, times(2)).call(any(Prompt.class));
        verify(jobs).fail(9L, "Missing middle category must be '-'");
    }

    private CategoryClassificationService service() {
        return new CategoryClassificationService(jobs, ChatClient.builder(chatModel),
                new ByteArrayResource("system".getBytes()),
                new ByteArrayResource("classification".getBytes()), "vision-model", "v1.0");
    }
}
