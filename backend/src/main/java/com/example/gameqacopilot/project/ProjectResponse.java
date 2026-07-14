package com.example.gameqacopilot.project;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String gameGenre,
        String platform,
        ProjectStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOwner().getId(),
                project.getName(),
                project.getDescription(),
                project.getGameGenre(),
                project.getPlatform(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
