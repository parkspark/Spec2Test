package com.example.gameqacopilot.output;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.analysis.loop.ActorEvaluatorLoop;
import com.example.gameqacopilot.ambiguity.Ambiguity;
import com.example.gameqacopilot.ambiguity.AmbiguityRepository;
import com.example.gameqacopilot.document.entity.DocumentProcessingStatus;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.document.repository.PlanningDocumentRepository;
import com.example.gameqacopilot.project.Project;
import com.example.gameqacopilot.project.ProjectRepository;
import com.example.gameqacopilot.requirement.Requirement;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.example.gameqacopilot.testcase.TestCase;
import com.example.gameqacopilot.testcase.TestCaseRepository;
import com.example.gameqacopilot.testcase.TestCaseStatus;
import com.example.gameqacopilot.user.User;
import com.example.gameqacopilot.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CsvOutputServiceTest {
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final PlanningDocumentRepository documents = mock(PlanningDocumentRepository.class);
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final AmbiguityRepository ambiguities = mock(AmbiguityRepository.class);
    private final RequirementRepository requirements = mock(RequirementRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OutputRepository outputs = mock(OutputRepository.class);
    private final CsvGenerator generator = mock(CsvGenerator.class);
    private final MarkdownGenerator markdownGenerator = mock(MarkdownGenerator.class);
    private final ActorEvaluatorLoop loop = mock(ActorEvaluatorLoop.class);
    private final CsvOutputService service = new CsvOutputService(
            projects, documents, testCases, ambiguities, requirements, users, outputs,
            generator, markdownGenerator, loop);

    @Test
    void createsOutputFromLatestReadyDocumentsApprovedCases() {
        Project project = mock(Project.class);
        PlanningDocument document = mock(PlanningDocument.class);
        User user = mock(User.class);
        List<TestCase> approved = List.of(mock(TestCase.class));
        when(project.getId()).thenReturn(1L);
        when(document.getId()).thenReturn(7L);
        when(projects.findById(1L)).thenReturn(Optional.of(project));
        when(documents.findFirstByProject_IdAndProcessingStatusOrderByCreatedAtDesc(
                1L, DocumentProcessingStatus.READY)).thenReturn(Optional.of(document));
        when(testCases.findAllByPlanningDocument_IdAndStatusOrderByDisplayOrder(7L, TestCaseStatus.APPROVED))
                .thenReturn(approved);
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(generator.generate(approved)).thenReturn("\uFEFFcsv");
        Output[] saved = new Output[1];
        when(outputs.save(any(Output.class))).thenAnswer(invocation -> {
            Output output = invocation.getArgument(0);
            ReflectionTestUtils.setField(output, "id", 9L);
            saved[0] = output;
            return output;
        });
        when(outputs.findById(9L)).thenAnswer(invocation -> Optional.of(saved[0]));

        service.create(1L, 2L);

        verify(testCases).findAllByPlanningDocument_IdAndStatusOrderByDisplayOrder(7L, TestCaseStatus.APPROVED);
        verify(loop).run(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsProjectWithoutReadyDocumentBeforeCreatingOutput() {
        when(projects.findById(1L)).thenReturn(Optional.of(mock(Project.class)));
        when(documents.findFirstByProject_IdAndProcessingStatusOrderByCreatedAtDesc(
                1L, DocumentProcessingStatus.READY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ready planning document not found");
        verify(outputs, never()).save(any());
    }

    @Test
    void createsMarkdownForLatestReadyDocumentsAmbiguities() {
        Project project = mock(Project.class);
        PlanningDocument document = mock(PlanningDocument.class);
        User user = mock(User.class);
        List<Ambiguity> documentAmbiguities = List.of(mock(Ambiguity.class));
        List<Requirement> documentRequirements = List.of(mock(Requirement.class));
        when(project.getId()).thenReturn(1L);
        when(document.getId()).thenReturn(7L);
        when(projects.findById(1L)).thenReturn(Optional.of(project));
        when(documents.findFirstByProject_IdAndProcessingStatusOrderByCreatedAtDesc(
                1L, DocumentProcessingStatus.READY)).thenReturn(Optional.of(document));
        when(ambiguities.findAllByPlanningDocument_IdOrderById(7L)).thenReturn(documentAmbiguities);
        when(requirements.findAllByAnalysisJob_PlanningDocument_Id(7L)).thenReturn(documentRequirements);
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(markdownGenerator.generate(any(), any(), any(), any(), any())).thenReturn("# markdown");
        Output[] saved = new Output[1];
        when(outputs.save(any(Output.class))).thenAnswer(invocation -> {
            Output output = invocation.getArgument(0);
            ReflectionTestUtils.setField(output, "id", 10L);
            saved[0] = output;
            return output;
        });
        when(outputs.findById(10L)).thenAnswer(invocation -> Optional.of(saved[0]));

        OutputResponse response = service.createMarkdown(1L, 2L);

        verify(ambiguities).findAllByPlanningDocument_IdOrderById(7L);
        verify(requirements).findAllByAnalysisJob_PlanningDocument_Id(7L);
        verify(loop).run(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        org.assertj.core.api.Assertions.assertThat(response.outputType()).isEqualTo("MARKDOWN_EXPORT");
        org.assertj.core.api.Assertions.assertThat(response.fileName()).isEqualTo("ambiguities-1.md");
    }
}
