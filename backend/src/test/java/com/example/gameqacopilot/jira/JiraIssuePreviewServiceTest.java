package com.example.gameqacopilot.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.ambiguity.Ambiguity;
import com.example.gameqacopilot.ambiguity.AmbiguityRepository;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JiraIssuePreviewServiceTest {
    @Test
    void buildsPreviewWithQuestionImpactEvidenceAndRelatedRequirements() {
        var ambiguities = mock(AmbiguityRepository.class);
        var requirements = mock(RequirementRepository.class);
        var ambiguity = mock(Ambiguity.class);
        var requirement = mock(Requirement.class);
        when(ambiguities.findById(3L)).thenReturn(Optional.of(ambiguity));
        when(ambiguity.getAnalysisJobId()).thenReturn(9L);
        when(ambiguity.getMajorCategory()).thenReturn("상점");
        when(ambiguity.getMiddleCategory()).thenReturn("무료 뽑기");
        when(ambiguity.getMinorCategory()).thenReturn("초기화");
        when(ambiguity.getTitle()).thenReturn("초기화 시간 기준 확인");
        when(ambiguity.getDescription()).thenReturn("초기화 기준이 정의되어 있지 않습니다.");
        when(ambiguity.getQuestion()).thenReturn("서버 시간 기준인가요?");
        when(ambiguity.getImpact()).thenReturn("경계 테스트를 확정할 수 없습니다.");
        when(ambiguity.getRelatedRequirementIds()).thenReturn("[\"REQ-001\"]");
        when(ambiguity.getEvidences()).thenReturn("""
                [{"evidenceType":"INFERRED","verificationStatus":"EXACT","pageNumber":3,
                  "sectionTitle":"무료 뽑기","sourceElementId":null,
                  "sourceText":"무료 뽑기는 매일 초기화된다.","sourceElementType":"TEXT",
                  "boundingBox":null,"reason":"시간 기준 없음"}]
                """);
        when(requirements.findAllByAnalysisJob_Id(9L)).thenReturn(List.of(requirement));
        when(requirement.getExternalRequirementId()).thenReturn("REQ-001");
        when(requirement.getTitle()).thenReturn("일일 무료 뽑기 제공");

        var preview = new JiraIssuePreviewService(ambiguities, requirements, new ObjectMapper()).preview(3L);

        assertThat(preview.title()).isEqualTo("[QA 확인 필요] 초기화 시간 기준 확인");
        assertThat(preview.body()).contains("## 관련 기능", "상점 > 무료 뽑기 > 초기화",
                "## 질문\n서버 시간 기준인가요?", "## 테스트 영향", "페이지: 3",
                "무료 뽑기는 매일 초기화된다.", "REQ-001 일일 무료 뽑기 제공", "Spec2Test");
    }

    @Test
    void mockClientPublishesToLocalIssueUrl() {
        var result = new MockJiraClient().publish(
                new JiraIssueRequest("AMB-001", "title", "body"));

        assertThat(result).isEqualTo(
                new JiraIssueResult("MOCK-AMB-001", "/mock-jira/issues/MOCK-AMB-001"));
    }
}
