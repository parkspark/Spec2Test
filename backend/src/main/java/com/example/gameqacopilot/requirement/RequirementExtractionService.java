package com.example.gameqacopilot.requirement;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.analysis.dto.RequirementExtractionResponse;
import com.example.gameqacopilot.analysis.service.AnalysisJobService;
import com.example.gameqacopilot.analysis.validator.AnalysisResultValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.HashSet;
import java.util.stream.IntStream;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Service
public class RequirementExtractionService {
    private final AnalysisJobService jobs;
    private final RequirementRepository requirements;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Resource systemPrompt;
    private final Resource requirementPrompt;

    public RequirementExtractionService(AnalysisJobService jobs, RequirementRepository requirements,
            ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
            @Value("classpath:/prompts/v1.0/system.txt") Resource systemPrompt,
            @Value("classpath:/prompts/v1.0/requirement.txt") Resource requirementPrompt) {
        this.jobs = jobs;
        this.requirements = requirements;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.systemPrompt = systemPrompt;
        this.requirementPrompt = requirementPrompt;
    }

    public RequirementExtractionResponse extract(Long jobId, List<AiAnalysisResponse.CategoryTree> categoryTree) {
        var input = jobs.prepareRequirements(jobId);
        try {
            return extract(jobId, categoryTree, input);
        } catch (Exception exception) {
            jobs.fail(jobId, exception.getMessage());
            throw new IllegalStateException("Requirement extraction failed", exception);
        }
    }

    private RequirementExtractionResponse extract(Long jobId,
            List<AiAnalysisResponse.CategoryTree> categoryTree,
            AnalysisJobService.RequirementInput input) throws Exception {
        var document = input.document();
        String request = requirementPrompt.getContentAsString(StandardCharsets.UTF_8)
                + "\n\n확정된 분류 체계 JSON:\n" + objectMapper.writeValueAsString(categoryTree)
                + "\n\nPDF 추출 텍스트:\n" + document.extractedText()
                + "\n\n페이지 요소 JSON:\n" + document.pageContents();
        var media = pageImages(document);
        var response = AnalysisResultValidator.validateWithOneRetry(
                () -> chatClient.prompt().system(systemPrompt)
                        .user(user -> user.text(request).media(media)).call()
                        .responseEntity(RequirementExtractionResponse.class),
                value -> validate(value.entity(), categoryTree));
        requirements.saveAll(response.entity().requirements().stream()
                .map(value -> toEntity(input.job(), value)).toList());
        var chatResponse = response.response();
        var usage = chatResponse.getMetadata().getUsage();
        jobs.recordRequirements(jobId, chatResponse.getResult().getOutput().getText(),
                usage == null || usage.getTotalTokens() == null ? null : usage.getTotalTokens().longValue());
        return response.entity();
    }

    private Media[] pageImages(AnalysisJobService.AnalysisInput document) {
        return IntStream.rangeClosed(1, document.pageCount())
                .mapToObj(page -> new FileSystemResource(Path.of(document.storedFilePath())
                        .resolveSibling("page-" + page + ".png")))
                .peek(image -> {
                    if (!image.exists()) throw new IllegalStateException("Document page image not found");
                })
                .map(image -> new Media(MimeTypeUtils.IMAGE_PNG, image))
                .toArray(Media[]::new);
    }

    private void validate(RequirementExtractionResponse response,
            List<AiAnalysisResponse.CategoryTree> categoryTree) {
        if (response == null || response.requirements() == null || response.requirements().isEmpty()) {
            throw new IllegalArgumentException("Requirements are empty");
        }
        var requirementIds = new HashSet<String>();
        for (var requirement : response.requirements()) {
            boolean knownCategory = categoryTree.stream().anyMatch(major ->
                    major.majorCategory().equals(requirement.majorCategory())
                    && major.middleCategories().stream().anyMatch(middle ->
                            middle.name().equals(requirement.middleCategory())
                            && middle.minorCategories().contains(requirement.minorCategory())));
            if (!knownCategory) throw new IllegalArgumentException("Requirement category is not in category tree");
            if (blank(requirement.requirementId()) || !requirementIds.add(requirement.requirementId())
                    || blank(requirement.title()) || blank(requirement.description())
                    || blank(requirement.actor()) || requirement.preconditions() == null
                    || blank(requirement.trigger())
                    || requirement.expectedBehaviors() == null || requirement.expectedBehaviors().isEmpty()
                    || requirement.expectedBehaviors().stream().anyMatch(this::blank)
                    || requirement.evidences() == null || requirement.evidences().isEmpty()
                    || requirement.evidences().stream().anyMatch(AnalysisResultValidator::invalidEvidence)) {
                throw new IllegalArgumentException("Requirement fields and evidence are invalid");
            }
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private Requirement toEntity(com.example.gameqacopilot.analysis.entity.AnalysisJob job,
            AiAnalysisResponse.Requirement value) {
        try {
            return new Requirement(job, value.requirementId(), value.majorCategory(), value.middleCategory(),
                    value.minorCategory(), value.title(), value.description(), value.actor(),
                    objectMapper.writeValueAsString(value.preconditions()), value.trigger(),
                    objectMapper.writeValueAsString(value.expectedBehaviors()), "[]",
                    objectMapper.writeValueAsString(value.evidences()));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Requirement JSON serialization failed", exception);
        }
    }
}
