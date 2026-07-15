package com.example.gameqacopilot.testcase;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestCaseNotesTest {
    @Test
    void addsEvidenceNotesAndPreservesExistingNotesWithoutDuplicates() {
        var notes = TestCaseNotes.merge(List.of("기대결과 정책 미정", "AI 추론 포함"), List.of(
                evidence(AiAnalysisResponse.EvidenceType.INFERRED,
                        AiAnalysisResponse.VerificationStatus.SIMILAR),
                evidence(AiAnalysisResponse.EvidenceType.UNSUPPORTED,
                        AiAnalysisResponse.VerificationStatus.NOT_FOUND)));

        assertThat(notes).containsExactly(
                "기대결과 정책 미정",
                "AI 추론 포함",
                "기획서 직접 근거 없음",
                "원문 유사 일치, QA 확인 필요",
                "원문 확인 불가");
    }

    private AiAnalysisResponse.Evidence evidence(AiAnalysisResponse.EvidenceType type,
            AiAnalysisResponse.VerificationStatus status) {
        return new AiAnalysisResponse.Evidence(type, status, 1, "정책", "PAGE-1-TEXT-1", "원문",
                AiAnalysisResponse.SourceElementType.TEXT, null, "근거");
    }
}