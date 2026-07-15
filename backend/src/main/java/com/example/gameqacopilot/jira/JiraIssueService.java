package com.example.gameqacopilot.jira;

import com.example.gameqacopilot.ambiguity.Ambiguity;
import com.example.gameqacopilot.ambiguity.AmbiguityRepository;
import com.example.gameqacopilot.ambiguity.AmbiguityStatus;
import com.example.gameqacopilot.output.Output;
import com.example.gameqacopilot.output.OutputRepository;
import com.example.gameqacopilot.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JiraIssueService {
    private final AmbiguityRepository ambiguities;
    private final UserRepository users;
    private final OutputRepository outputs;
    private final JiraIssuePreviewService previews;
    private final JiraClient jiraClient;
    private final ObjectMapper objectMapper;

    public JiraIssueService(AmbiguityRepository ambiguities, UserRepository users,
            OutputRepository outputs, JiraIssuePreviewService previews,
            JiraClient jiraClient, ObjectMapper objectMapper) {
        this.ambiguities = ambiguities;
        this.users = users;
        this.outputs = outputs;
        this.previews = previews;
        this.jiraClient = jiraClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public JiraIssuePublicationResponse publish(Long ambiguityId, Long userId) {
        Ambiguity ambiguity = ambiguities.findForUpdateById(ambiguityId)
                .orElseThrow(() -> new NoSuchElementException("Ambiguity not found"));
        if (ambiguity.getStatus() != AmbiguityStatus.OPEN || ambiguity.getJiraIssueKey() != null) {
            throw new IllegalArgumentException("Jira issue already created");
        }
        var user = users.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        JiraIssuePreview preview = previews.preview(ambiguityId);
        var request = new JiraIssueRequest(ambiguity.getExternalAmbiguityId(), preview.title(), preview.body());
        String requestJson = json(request);
        var document = ambiguity.getPlanningDocument();
        Output output = outputs.save(new Output(
                document.getProject(), document, ambiguity, user, requestJson));

        try {
            JiraIssueResult result = jiraClient.publish(request);
            if (result.issueKey() == null || result.issueKey().isBlank()
                    || result.issueUrl() == null || result.issueUrl().isBlank()) {
                throw new IllegalStateException("Jira response is missing issue key or URL");
            }
            output.succeedJira(requestJson, json(result), result.issueKey(), result.issueUrl());
            ambiguity.issueCreated(result.issueKey(), result.issueUrl());
        } catch (RuntimeException exception) {
            output.fail(exception.getMessage() == null ? "Jira issue creation failed" : exception.getMessage());
        }
        return new JiraIssuePublicationResponse(output.getId(), ambiguityId, output.getStatus(),
                output.getExternalResourceId(), output.getExternalUrl(), output.getFailureReason());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Jira request data serialization failed", exception);
        }
    }
}
