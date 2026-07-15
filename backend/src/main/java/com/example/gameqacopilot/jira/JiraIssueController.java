package com.example.gameqacopilot.jira;

import com.example.gameqacopilot.common.response.ApiResponse;
import com.example.gameqacopilot.common.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JiraIssueController {
    private final JiraIssuePreviewService previews;
    private final JiraIssueService issues;

    public JiraIssueController(JiraIssuePreviewService previews, JiraIssueService issues) {
        this.previews = previews;
        this.issues = issues;
    }

    @GetMapping("/api/ambiguities/{ambiguityId}/jira-issue/preview")
    ApiResponse<JiraIssuePreview> preview(@PathVariable Long ambiguityId) {
        return ApiResponse.of(previews.preview(ambiguityId));
    }


    @PostMapping("/api/ambiguities/{ambiguityId}/jira-issue")
    ApiResponse<JiraIssuePublicationResponse> publish(
            @PathVariable Long ambiguityId, @AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.of(issues.publish(ambiguityId, user.id()));
    }
}
