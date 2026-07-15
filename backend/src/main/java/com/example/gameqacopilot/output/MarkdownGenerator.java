package com.example.gameqacopilot.output;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.ambiguity.Ambiguity;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.project.Project;
import com.example.gameqacopilot.requirement.Requirement;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MarkdownGenerator {
    private final ObjectMapper objectMapper;

    public MarkdownGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generate(
            Project project,
            PlanningDocument document,
            List<Ambiguity> ambiguities,
            List<Requirement> requirements,
            LocalDate generatedDate) {
        Map<String, Requirement> requirementByKey = requirements.stream().collect(Collectors.toMap(
                requirement -> key(requirement.getAnalysisJobId(), requirement.getExternalRequirementId()),
                Function.identity()));
        StringBuilder markdown = new StringBuilder("# 모호한 요구사항 목록\n\n")
                .append("## 프로젝트 정보\n")
                .append("- 프로젝트명: ").append(project.getName()).append('\n')
                .append("- 기획서명: ").append(document.getTitle()).append('\n')
                .append("- 생성일: ").append(generatedDate).append('\n');

        for (Ambiguity ambiguity : ambiguities) {
            markdown.append("\n## ").append(ambiguity.getExternalAmbiguityId()).append(' ')
                    .append(ambiguity.getTitle()).append("\n\n")
                    .append("- 관련 요구사항: ")
                    .append(relatedRequirements(ambiguity, requirementByKey)).append('\n')
                    .append("- 심각도: ").append(ambiguity.getSeverity()).append('\n')
                    .append("- 상태: ").append(ambiguity.getStatus()).append("\n\n")
                    .append("### 질문\n").append(ambiguity.getQuestion()).append("\n\n")
                    .append("### 테스트 영향\n").append(ambiguity.getImpact()).append("\n\n")
                    .append("### 기획서 근거\n")
                    .append(evidences(ambiguity.getEvidences()));
        }
        return markdown.toString();
    }

    private String relatedRequirements(Ambiguity ambiguity, Map<String, Requirement> requirementByKey) {
        List<String> ids = read(ambiguity.getRelatedRequirementIds(), new TypeReference<List<String>>() {});
        if (ids == null || ids.isEmpty()) {
            return "-";
        }
        return ids.stream().map(id -> {
            Requirement requirement = requirementByKey.get(key(ambiguity.getAnalysisJobId(), id));
            return requirement == null ? id : id + " " + requirement.getTitle();
        }).collect(Collectors.joining(", "));
    }

    private String evidences(String json) {
        List<AiAnalysisResponse.Evidence> evidences = read(
                json, new TypeReference<List<AiAnalysisResponse.Evidence>>() {});
        if (evidences == null || evidences.isEmpty()) {
            return "- 페이지: - / 근거 유형: UNSUPPORTED\n- 원문: -\n";
        }
        return evidences.stream()
                .map(evidence -> "- 페이지: " + evidence.pageNumber() + " / 근거 유형: "
                        + evidence.evidenceType() + "\n- 원문: " + evidence.sourceText() + "\n")
                .collect(Collectors.joining("\n"));
    }

    private String key(Long analysisJobId, String externalRequirementId) {
        return analysisJobId + ":" + externalRequirementId;
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored ambiguity data is invalid", exception);
        }
    }
}
