package com.example.gameqacopilot.testcase;

import jakarta.validation.constraints.NotBlank;

public record TestCaseRejectRequest(@NotBlank String reason) {}
