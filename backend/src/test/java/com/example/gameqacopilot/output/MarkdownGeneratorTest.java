package com.example.gameqacopilot.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.ambiguity.Ambiguity;
import com.example.gameqacopilot.ambiguity.AmbiguityStatus;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.project.Project;
import com.example.gameqacopilot.requirement.Requirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownGeneratorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MarkdownGenerator generator = new MarkdownGenerator(objectMapper);

    @Test
    void writesProjectQuestionImpactSeverityRequirementsAndEvidence() throws Exception {
        Project project = mock(Project.class);
        PlanningDocument document = mock(PlanningDocument.class);
        Ambiguity ambiguity = mock(Ambiguity.class);
        Requirement requirement = mock(Requirement.class);
        when(project.getName()).thenReturn("Game Sample");
        when(document.getTitle()).thenReturn("무료 뽑기 시스템");
        when(ambiguity.getAnalysisJobId()).thenReturn(3L);
        when(ambiguity.getExternalAmbiguityId()).thenReturn("AMB-001");
        when(ambiguity.getTitle()).thenReturn("무료 뽑기 초기화 시간 기준 불명확");
        when(ambiguity.getQuestion()).thenReturn("무료 뽑기 횟수는 서버 시간 기준으로 초기화되나요?");
        when(ambiguity.getImpact()).thenReturn("초기화 경계 테스트의 예상 결과를 확정할 수 없습니다.");
        when(ambiguity.getSeverity()).thenReturn("HIGH");
        when(ambiguity.getStatus()).thenReturn(AmbiguityStatus.OPEN);
        when(ambiguity.getRelatedRequirementIds()).thenReturn(objectMapper.writeValueAsString(List.of("REQ-001")));
        when(ambiguity.getEvidences()).thenReturn(objectMapper.writeValueAsString(List.of(evidence())));
        when(requirement.getAnalysisJobId()).thenReturn(3L);
        when(requirement.getExternalRequirementId()).thenReturn("REQ-001");
        when(requirement.getTitle()).thenReturn("일일 무료 뽑기 제공");

        String markdown = generator.generate(
                project, document, List.of(ambiguity), List.of(requirement), LocalDate.of(2026, 7, 15));

        assertThat(markdown).isEqualTo("""
                # 모호한 요구사항 목록

                ## 프로젝트 정보
                - 프로젝트명: Game Sample
                - 기획서명: 무료 뽑기 시스템
                - 생성일: 2026-07-15

                ## AMB-001 무료 뽑기 초기화 시간 기준 불명확

                - 관련 요구사항: REQ-001 일일 무료 뽑기 제공
                - 심각도: HIGH
                - 상태: OPEN

                ### 질문
                무료 뽑기 횟수는 서버 시간 기준으로 초기화되나요?

                ### 테스트 영향
                초기화 경계 테스트의 예상 결과를 확정할 수 없습니다.

                ### 기획서 근거
                - 페이지: 3 / 근거 유형: INFERRED
                - 원문: 무료 뽑기는 매일 초기화된다.
                """);
    }

    private AiAnalysisResponse.Evidence evidence() {
        return new AiAnalysisResponse.Evidence(
                AiAnalysisResponse.EvidenceType.INFERRED,
                AiAnalysisResponse.VerificationStatus.EXACT,
                3,
                "무료 뽑기 정책",
                null,
                "무료 뽑기는 매일 초기화된다.",
                AiAnalysisResponse.SourceElementType.TEXT,
                null,
                "초기화 기준 시간은 정의되지 않았다.");
    }
}
