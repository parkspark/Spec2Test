package com.example.gameqacopilot.jira;

import com.example.gameqacopilot.output.OutputStatus;

public record JiraIssuePublicationResponse(
        Long outputId,
        Long ambiguityId,
        OutputStatus status,
        String issueKey,
        String issueUrl,
        String failureReason) {}
