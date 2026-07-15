package com.example.gameqacopilot.analysis.service;

import com.example.gameqacopilot.analysis.dto.CategoryClassificationResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.IntStream;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Service
public class CategoryClassificationService {
    private final AnalysisJobService jobs;
    private final ChatClient chatClient;
    private final Resource systemPrompt;
    private final Resource classificationPrompt;
    private final String modelName;
    private final String promptVersion;

    public CategoryClassificationService(
            AnalysisJobService jobs,
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompts/v1.0/system.txt") Resource systemPrompt,
            @Value("classpath:/prompts/v1.0/classification.txt") Resource classificationPrompt,
            @Value("${app.ai.model}") String modelName,
            @Value("${app.ai.prompt-version:v1.0}") String promptVersion) {
        this.jobs = jobs;
        this.chatClient = chatClientBuilder.build();
        this.systemPrompt = systemPrompt;
        this.classificationPrompt = classificationPrompt;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
    }

    public CategoryClassificationResponse classify(Long jobId) {
        var input = jobs.startClassification(jobId, modelName, promptVersion);
        try {
            var media = IntStream.rangeClosed(1, input.pageCount())
                    .mapToObj(page -> new FileSystemResource(Path.of(input.storedFilePath())
                            .resolveSibling("page-" + page + ".png")))
                    .peek(image -> {
                        if (!image.exists()) throw new IllegalStateException("Document page image not found");
                    })
                    .map(image -> new Media(MimeTypeUtils.IMAGE_PNG, image))
                    .toArray(Media[]::new);
            String request = classificationPrompt.getContentAsString(StandardCharsets.UTF_8)
                    + "\n\nPDF 추출 텍스트:\n" + input.extractedText()
                    + "\n\n페이지 요소 JSON:\n" + input.pageContents();
            var response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(user -> user.text(request).media(media))
                    .call()
                    .responseEntity(CategoryClassificationResponse.class);
            validate(response.entity());
            var chatResponse = response.response();
            String raw = chatResponse.getResult().getOutput().getText();
            var usage = chatResponse.getMetadata().getUsage();
            jobs.recordClassification(jobId, raw,
                    usage == null || usage.getTotalTokens() == null ? null : usage.getTotalTokens().longValue());
            return response.entity();
        } catch (Exception exception) {
            jobs.fail(jobId, exception.getMessage());
            throw new IllegalStateException("Feature classification failed", exception);
        }
    }

    private void validate(CategoryClassificationResponse response) {
        if (response == null || response.categoryTree() == null || response.categoryTree().isEmpty()) {
            throw new IllegalArgumentException("Category tree is empty");
        }
        for (var major : response.categoryTree()) {
            if (major.majorCategory() == null || major.majorCategory().isBlank()) {
                throw new IllegalArgumentException("Major category is required");
            }
            if (major.middleCategories() == null || major.middleCategories().isEmpty()) {
                throw new IllegalArgumentException("Missing middle category must be '-'");
            }
            for (var middle : major.middleCategories()) {
                if (middle.name() == null || middle.name().isBlank()
                        || middle.minorCategories() == null || middle.minorCategories().isEmpty()
                        || middle.minorCategories().stream().anyMatch(value -> value == null || value.isBlank())) {
                    throw new IllegalArgumentException("Missing categories must be '-'");
                }
            }
        }
    }
}
