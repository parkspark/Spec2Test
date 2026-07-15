package com.example.gameqacopilot.jira;

import com.example.gameqacopilot.ambiguity.Ambiguity;
import com.example.gameqacopilot.ambiguity.AmbiguityRepository;
import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.Evidence;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JiraIssuePreviewService {
    private final AmbiguityRepository ambiguities;
    private final RequirementRepository requirements;
    private final ObjectMapper objectMapper;

    public JiraIssuePreviewService(AmbiguityRepository ambiguities,
            RequirementRepository requirements, ObjectMapper objectMapper) {
        this.ambiguities = ambiguities;
        this.requirements = requirements;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public JiraIssuePreview preview(Long ambiguityId) {
        Ambiguity ambiguity = ambiguities.findById(ambiguityId)
                .orElseThrow(() -> new NoSuchElementException("Ambiguity not found"));
        var requirementById = requirements.findAllByAnalysisJob_Id(ambiguity.getAnalysisJobId()).stream()
                .collect(Collectors.toMap(Requirement::getExternalRequirementId, Function.identity()));
        List<String> relatedIds = read(ambiguity.getRelatedRequirementIds(), new TypeReference<>() {});
        List<Evidence> evidences = read(ambiguity.getEvidences(), new TypeReference<>() {});
        String related = relatedIds.stream().map(id -> {
            Requirement requirement = requirementById.get(id);
            return requirement == null ? id : id + " " + requirement.getTitle();
        }).collect(Collectors.joining("\n"));
        String evidence = evidences.stream()
                .map(value -> "- 페이지: " + (value.pageNumber() == null ? "-" : value.pageNumber())
                        + "\n- 원문: " + value.sourceText())
                .collect(Collectors.joining("\n"));
        String feature = List.of(ambiguity.getMajorCategory(), ambiguity.getMiddleCategory(),
                        ambiguity.getMinorCategory()).stream()
                .filter(value -> !value.isBlank() && !"-".equals(value))
                .collect(Collectors.joining(" > "));
        String body = "## 관련 기능\n" + feature
                + "\n\n## 확인이 필요한 내용\n" + ambiguity.getDescription()
                + "\n\n## 질문\n" + ambiguity.getQuestion()
                + "\n\n## 테스트 영향\n" + ambiguity.getImpact()
                + "\n\n## 기획서 근거\n" + evidence
                + "\n\n## 관련 요구사항\n" + related
                + "\n\n## 생성 출처\nSpec2Test";
        return new JiraIssuePreview("[QA 확인 필요] " + ambiguity.getTitle(), body);
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored ambiguity data is invalid", exception);
        }
    }
}
