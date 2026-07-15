package com.example.gameqacopilot.jira;

import com.example.gameqacopilot.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JiraIssueController {
    private final JiraIssuePreviewService previews;

    public JiraIssueController(JiraIssuePreviewService previews) {
        this.previews = previews;
    }

    @GetMapping("/api/ambiguities/{ambiguityId}/jira-issue/preview")
    ApiResponse<JiraIssuePreview> preview(@PathVariable Long ambiguityId) {
        return ApiResponse.of(previews.preview(ambiguityId));
    }
}
