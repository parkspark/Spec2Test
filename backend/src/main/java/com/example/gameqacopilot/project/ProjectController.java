package com.example.gameqacopilot.project;

import com.example.gameqacopilot.common.response.ApiResponse;
import com.example.gameqacopilot.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
class ProjectController {
    private final ProjectService projectService;

    ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    ApiResponse<ProjectResponse> create(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.of(projectService.create(user.id(), request));
    }

    @GetMapping
    ApiResponse<List<ProjectResponse>> findAll() {
        return ApiResponse.of(projectService.findAll());
    }

    @GetMapping("/{projectId}")
    ApiResponse<ProjectResponse> findById(@PathVariable Long projectId) {
        return ApiResponse.of(projectService.findById(projectId));
    }
}
