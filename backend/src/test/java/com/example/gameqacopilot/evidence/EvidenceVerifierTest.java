package com.example.gameqacopilot.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvidenceVerifierTest {
    private final EvidenceVerifier verifier = new EvidenceVerifier(new ObjectMapper());

    @Test
    void classifiesNormalizedPageTextAndForcesUnsupportedWhenNotFound() {
        assertThat(verify("free  draw", " free\ndraw ").verificationStatus()).isEqualTo(status("EXACT"));
        assertThat(verify("free draw", "daily free draw available").verificationStatus()).isEqualTo(status("PARTIAL"));
        assertThat(verify("free draw available", "free draw avai1able").verificationStatus()).isEqualTo(status("SIMILAR"));

        var missing = verify("free draw", "unrelated policy");

        assertThat(missing.verificationStatus()).isEqualTo(status("NOT_FOUND"));
        assertThat(missing.evidenceType()).isEqualTo(AiAnalysisResponse.EvidenceType.UNSUPPORTED);
        assertThat(verifier.requiresHumanReview(List.of(missing))).isTrue();
    }

    @Test
    void rejectsPageOutsideDocumentRange() {
        assertThatThrownBy(() -> verifier.verify(List.of(evidence("source", 2)), page("source"), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside document range");
    }

    private AiAnalysisResponse.Evidence verify(String source, String page) {
        return verifier.verify(List.of(evidence(source, 1)), page(page), 1).getFirst();
    }

    private AiAnalysisResponse.Evidence evidence(String source, int pageNumber) {
        return new AiAnalysisResponse.Evidence(AiAnalysisResponse.EvidenceType.EXPLICIT, null, pageNumber,
                "policy", null, source, AiAnalysisResponse.SourceElementType.TEXT, null, "reason");
    }

    private String page(String text) {
        return """
                [{"pageNumber":1,"elements":[{"elementId":"PAGE-1-TEXT-01","elementType":"TEXT",
                "text":"%s","boundingBox":{"x":0,"y":0,"width":1,"height":1}}]}]
                """.formatted(text.replace("\n", "\\n"));
    }

    private AiAnalysisResponse.VerificationStatus status(String value) {
        return AiAnalysisResponse.VerificationStatus.valueOf(value);
    }
}
