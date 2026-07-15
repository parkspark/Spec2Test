package com.example.gameqacopilot.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.ambiguity.Ambiguity;
import com.example.gameqacopilot.ambiguity.AmbiguityRepository;
import com.example.gameqacopilot.ambiguity.AmbiguityStatus;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.output.Output;
import com.example.gameqacopilot.output.OutputRepository;
import com.example.gameqacopilot.output.OutputStatus;
import com.example.gameqacopilot.project.Project;
import com.example.gameqacopilot.user.User;
import com.example.gameqacopilot.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JiraIssueServiceTest {
    private final AmbiguityRepository ambiguities = mock(AmbiguityRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OutputRepository outputs = mock(OutputRepository.class);
    private final JiraIssuePreviewService previews = mock(JiraIssuePreviewService.class);
    private final JiraClient jiraClient = mock(JiraClient.class);
    private final JiraIssueService service = new JiraIssueService(
            ambiguities, users, outputs, previews, jiraClient, new ObjectMapper());
    private final Ambiguity ambiguity = mock(Ambiguity.class);
    private Output savedOutput;

    @BeforeEach
    void setUp() {
        var document = mock(PlanningDocument.class);
        var project = mock(Project.class);
        when(ambiguities.findForUpdateById(3L)).thenReturn(Optional.of(ambiguity));
        when(ambiguity.getStatus()).thenReturn(AmbiguityStatus.OPEN);
        when(ambiguity.getExternalAmbiguityId()).thenReturn("AMB-001");
        when(ambiguity.getPlanningDocument()).thenReturn(document);
        when(document.getProject()).thenReturn(project);
        when(users.findById(2L)).thenReturn(Optional.of(mock(User.class)));
        when(previews.preview(3L)).thenReturn(new JiraIssuePreview("title", "body"));
        when(outputs.save(any(Output.class))).thenAnswer(invocation -> {
            Output output = invocation.getArgument(0);
            ReflectionTestUtils.setField(output, "id", 10L);
            savedOutput = output;
            return output;
        });
    }

    @Test
    void publishesOpenAmbiguityAndStoresOutputData() {
        when(jiraClient.publish(any())).thenReturn(new JiraIssueResult("MOCK-AMB-001", "/issues/MOCK-AMB-001"));

        JiraIssuePublicationResponse response = service.publish(3L, 2L);

        assertThat(response.status()).isEqualTo(OutputStatus.SUCCESS);
        assertThat(response.issueKey()).isEqualTo("MOCK-AMB-001");
        assertThat(savedOutput.getOutputType()).isEqualTo("JIRA_ISSUE");
        assertThat(savedOutput.getRequestData()).contains("AMB-001", "title", "body");
        assertThat(savedOutput.getResponseData()).contains("MOCK-AMB-001", "/issues/MOCK-AMB-001");
        verify(jiraClient).publish(new JiraIssueRequest("AMB-001", "title", "body"));
        verify(ambiguity).issueCreated("MOCK-AMB-001", "/issues/MOCK-AMB-001");
        verify(outputs).save(any(Output.class));
    }

    @Test
    void storesFailedOutputAndLeavesAmbiguityRetryable() {
        when(jiraClient.publish(any())).thenThrow(new IllegalStateException("Jira unavailable"));

        JiraIssuePublicationResponse response = service.publish(3L, 2L);

        assertThat(response.status()).isEqualTo(OutputStatus.FAILED);
        assertThat(response.failureReason()).isEqualTo("Jira unavailable");
        verify(ambiguity, never()).issueCreated(any(), any());
    }

    @Test
    void blocksDuplicateIssueBeforePublishing() {
        when(ambiguity.getStatus()).thenReturn(AmbiguityStatus.ISSUE_CREATED);

        assertThatThrownBy(() -> service.publish(3L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Jira issue already created");
        verify(jiraClient, never()).publish(any());
        verify(outputs, never()).save(any());
    }
}
