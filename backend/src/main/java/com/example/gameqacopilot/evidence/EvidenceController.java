package com.example.gameqacopilot.evidence;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.Evidence;
import com.example.gameqacopilot.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvidenceController {
    private final EvidenceService evidences;

    public EvidenceController(EvidenceService evidences) {
        this.evidences = evidences;
    }

    @GetMapping("/api/test-cases/{id}/evidences")
    ApiResponse<List<Evidence>> findByTestCase(@PathVariable Long id) {
        return ApiResponse.of(evidences.findByTestCase(id));
    }

    @GetMapping("/api/requirements/{id}/evidences")
    ApiResponse<List<Evidence>> findByRequirement(@PathVariable Long id) {
        return ApiResponse.of(evidences.findByRequirement(id));
    }

    @GetMapping("/api/ambiguities/{id}/evidences")
    ApiResponse<List<Evidence>> findByAmbiguity(@PathVariable Long id) {
        return ApiResponse.of(evidences.findByAmbiguity(id));
    }
}
