package com.example.gameqacopilot.project;

import com.example.gameqacopilot.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProjectService {
    private final ProjectRepository projects;
    private final UserRepository users;

    ProjectService(ProjectRepository projects, UserRepository users) {
        this.projects = projects;
        this.users = users;
    }

    @Transactional
    ProjectResponse create(Long ownerId, ProjectCreateRequest request) {
        var owner = users.findById(ownerId).orElseThrow();
        return ProjectResponse.from(projects.save(new Project(owner, request)));
    }

    @Transactional(readOnly = true)
    List<ProjectResponse> findAll() {
        return projects.findAll().stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    ProjectResponse findById(Long projectId) {
        return ProjectResponse.from(projects.findById(projectId).orElseThrow());
    }
}
