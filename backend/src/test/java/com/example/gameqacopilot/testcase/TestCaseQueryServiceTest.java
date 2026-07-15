package com.example.gameqacopilot.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import com.example.gameqacopilot.project.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestCaseQueryServiceTest {
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final TestCaseQueryService service = new TestCaseQueryService(testCases, projects, new ObjectMapper());
    private TestCase testCase;

    @BeforeEach
    void setUp() {
        testCase = mock(TestCase.class);
        when(testCase.getId()).thenReturn(10L);
        when(testCase.getExternalTestCaseId()).thenReturn("TC-001");
        when(testCase.getAnalysisJobId()).thenReturn(20L);
        when(testCase.getRequirementId()).thenReturn(30L);
        when(testCase.getDisplayOrder()).thenReturn(1);
        when(testCase.getMajorCategory()).thenReturn("무료 뽑기");
        when(testCase.getMiddleCategory()).thenReturn("보상 지급");
        when(testCase.getMinorCategory()).thenReturn("일일 무료 횟수");
        when(testCase.getTestItem()).thenReturn("무료 뽑기 정상 사용");
        when(testCase.getTestType()).thenReturn(AiAnalysisResponse.TestType.HAPPY_PATH);
        when(testCase.getPriority()).thenReturn("HIGH");
        when(testCase.getConfidence()).thenReturn(AiAnalysisResponse.Confidence.HIGH);
        when(testCase.getStatus()).thenReturn(TestCaseStatus.GENERATED);
        when(testCase.getPreconditions()).thenReturn("[\"로그인 상태\"]");
        when(testCase.getTestSteps()).thenReturn("[{\"stepNumber\":1,\"action\":\"버튼 선택\",\"expectedResult\":\"결과 표시\"}]");
        when(testCase.getExpectedResults()).thenReturn("[\"보상 지급\"]");
        when(testCase.getEvidences()).thenReturn("[{\"evidenceType\":\"EXPLICIT\",\"verificationStatus\":\"EXACT\",\"pageNumber\":7,\"sectionTitle\":\"정책\",\"sourceText\":\"하루 한 번\",\"sourceElementType\":\"TEXT\"}]");
        when(testCase.getNotes()).thenReturn("[\"QA 검토 필수\"]");
        when(testCase.isRequiresHumanReview()).thenReturn(true);
    }

    @Test
    void returnsFilteredListWithEvidenceSummary() {
        when(projects.existsById(1L)).thenReturn(true);
        when(testCases.findAllFiltered(1L, 20L, TestCaseStatus.GENERATED,
                "무료 뽑기", null, null, AiAnalysisResponse.TestType.HAPPY_PATH,
                AiAnalysisResponse.Confidence.HIGH, "정상")).thenReturn(List.of(testCase));

        var response = service.findAll(1L, 20L, TestCaseStatus.GENERATED,
                " 무료 뽑기 ", " ", null, AiAnalysisResponse.TestType.HAPPY_PATH,
                AiAnalysisResponse.Confidence.HIGH, " 정상 ");

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.testItem()).isEqualTo("무료 뽑기 정상 사용");
            assertThat(item.evidenceSummary().pageNumber()).isEqualTo(7);
            assertThat(item.evidenceSummary().sectionTitle()).isEqualTo("정책");
            assertThat(item.testSteps()).singleElement()
                    .satisfies(step -> assertThat(step.expectedResult()).isEqualTo("결과 표시"));
        });
        verify(testCases).findAllFiltered(1L, 20L, TestCaseStatus.GENERATED,
                "무료 뽑기", null, null, AiAnalysisResponse.TestType.HAPPY_PATH,
                AiAnalysisResponse.Confidence.HIGH, "정상");
    }

    @Test
    void usesSimpleQueryWhenNoFiltersAreProvided() {
        when(projects.existsById(1L)).thenReturn(true);
        when(testCases.findAllByProject_IdOrderByDisplayOrder(1L)).thenReturn(List.of(testCase));

        var response = service.findAll(1L, null, null, null, null, null, null, null, null);

        assertThat(response.items()).hasSize(1);
        verify(testCases).findAllByProject_IdOrderByDisplayOrder(1L);
    }

    @Test
    void returnsDetailWithAllEvidences() {
        when(testCases.findById(10L)).thenReturn(Optional.of(testCase));

        var response = service.findById(10L);

        assertThat(response.evidences()).singleElement()
                .satisfies(evidence -> assertThat(evidence.sourceText()).isEqualTo("하루 한 번"));
        assertThat(response.preconditions()).containsExactly("로그인 상태");
        assertThat(response.notes()).containsExactly("QA 검토 필수");
    }

    @Test
    void rejectsUnknownProjectAndTestCase() {
        assertThatThrownBy(() -> service.findAll(99L, null, null, null, null, null, null, null, null))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessage("Project not found");
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessage("Test case not found");
    }
}
