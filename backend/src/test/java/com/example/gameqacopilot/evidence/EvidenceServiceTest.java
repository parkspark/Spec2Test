package com.example.gameqacopilot.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.ambiguity.Ambiguity;
import com.example.gameqacopilot.ambiguity.AmbiguityRepository;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.example.gameqacopilot.testcase.TestCase;
import com.example.gameqacopilot.testcase.TestCaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceServiceTest {
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final RequirementRepository requirements = mock(RequirementRepository.class);
    private final AmbiguityRepository ambiguities = mock(AmbiguityRepository.class);
    private final EvidenceService service = new EvidenceService(
            testCases, requirements, ambiguities, new ObjectMapper());

    private TestCase testCase;
    private Requirement requirement;
    private Ambiguity ambiguity;

    @BeforeEach
    void setUp() {
        testCase = mock(TestCase.class);
        requirement = mock(Requirement.class);
        ambiguity = mock(Ambiguity.class);
        when(testCase.getDocumentPageCount()).thenReturn(2);
        when(requirement.getDocumentPageCount()).thenReturn(2);
        when(ambiguity.getDocumentPageCount()).thenReturn(2);
        when(testCases.findById(1L)).thenReturn(Optional.of(testCase));
        when(requirements.findById(1L)).thenReturn(Optional.of(requirement));
        when(ambiguities.findById(1L)).thenReturn(Optional.of(ambiguity));
    }

    @Test
    void returnsStoredEvidenceForAllThreeOwners() {
        when(testCase.getEvidences()).thenReturn(evidence(1));
        when(requirement.getEvidences()).thenReturn(evidence(1));
        when(ambiguity.getEvidences()).thenReturn(evidence(1));

        assertThat(service.findByTestCase(1L)).singleElement()
                .satisfies(value -> assertThat(value.sourceText()).isEqualTo("Free draw once daily"));
        assertThat(service.findByRequirement(1L)).singleElement()
                .satisfies(value -> assertThat(value.pageNumber()).isEqualTo(1));
        assertThat(service.findByAmbiguity(1L)).singleElement()
                .satisfies(value -> assertThat(value.verificationStatus().name()).isEqualTo("EXACT"));
    }

    @Test
    void rejectsEvidencePageOutsideDocumentRange() {
        when(testCase.getEvidences()).thenReturn(evidence(3));

        assertThatThrownBy(() -> service.findByTestCase(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stored evidence page is outside the document range");
    }

    @Test
    void returnsNotFoundForUnknownOwner() {
        assertThatThrownBy(() -> service.findByRequirement(99L))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessage("Requirement not found");
    }

    private String evidence(int pageNumber) {
        return """
                [{
                  "evidenceType":"EXPLICIT",
                  "verificationStatus":"EXACT",
                  "pageNumber":%d,
                  "sectionTitle":"Free draw",
                  "sourceElementId":"PAGE-1-TEXT-01",
                  "sourceText":"Free draw once daily",
                  "sourceElementType":"TEXT",
                  "boundingBox":{"x":0.1,"y":0.2,"width":0.3,"height":0.1},
                  "reason":"Daily limit"
                }]
                """.formatted(pageNumber);
    }
}
