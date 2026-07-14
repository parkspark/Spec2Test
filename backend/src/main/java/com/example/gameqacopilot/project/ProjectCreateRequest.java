package com.example.gameqacopilot.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectCreateRequest(
        @NotBlank String name,
        String description,
        String gameGenre,
        String platform) {}
