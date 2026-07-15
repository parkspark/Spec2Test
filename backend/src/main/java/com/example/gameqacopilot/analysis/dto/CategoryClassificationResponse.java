package com.example.gameqacopilot.analysis.dto;

import java.util.List;

public record CategoryClassificationResponse(
        List<AiAnalysisResponse.CategoryTree> categoryTree,
        List<AiAnalysisResponse.Evidence> evidences) {}
