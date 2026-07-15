package com.example.gameqacopilot.jira;

import org.springframework.stereotype.Component;

@Component
public class MockJiraClient implements JiraClient {
    @Override
    public JiraIssueResult publish(JiraIssueRequest request) {
        String key = "MOCK-" + request.sourceId();
        return new JiraIssueResult(key, "/mock-jira/issues/" + key);
    }
}
