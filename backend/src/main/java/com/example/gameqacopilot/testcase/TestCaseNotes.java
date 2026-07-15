package com.example.gameqacopilot.testcase;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.Evidence;
import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.EvidenceType;
import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.VerificationStatus;
import java.util.ArrayList;
import java.util.List;

final class TestCaseNotes {
    private TestCaseNotes() {}

    static List<String> merge(List<String> existing, List<Evidence> evidences) {
        var notes = new ArrayList<>(existing);
        if (evidences.stream().anyMatch(e -> e.evidenceType() == EvidenceType.INFERRED)) {
            addOnce(notes, "AI 추론 포함");
        }
        if (evidences.stream().anyMatch(e -> e.evidenceType() == EvidenceType.UNSUPPORTED)) {
            addOnce(notes, "기획서 직접 근거 없음");
        }
        if (evidences.stream().anyMatch(e -> e.verificationStatus() == VerificationStatus.SIMILAR)) {
            addOnce(notes, "원문 유사 일치, QA 확인 필요");
        }
        if (evidences.stream().anyMatch(e -> e.verificationStatus() == VerificationStatus.NOT_FOUND)) {
            addOnce(notes, "원문 확인 불가");
        }
        return notes;
    }

    private static void addOnce(List<String> notes, String note) {
        if (!notes.contains(note)) notes.add(note);
    }
}