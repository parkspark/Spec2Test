package com.example.gameqacopilot.document.controller;

import com.example.gameqacopilot.common.response.ApiResponse;
import com.example.gameqacopilot.common.security.CurrentUser;
import com.example.gameqacopilot.document.dto.PlanningDocumentResponse;
import com.example.gameqacopilot.document.service.PlanningDocumentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/projects/{projectId}/documents")
public class PlanningDocumentController {
    private final PlanningDocumentService documentService;

    public PlanningDocumentController(PlanningDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    ApiResponse<List<PlanningDocumentResponse>> findAll(@PathVariable Long projectId) {
        return ApiResponse.of(documentService.findAll(projectId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<PlanningDocumentResponse> upload(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam @NotBlank @Size(max = 255) String title,
            @RequestParam MultipartFile file) {
        return ApiResponse.of(documentService.upload(projectId, user.id(), title, file));
    }
}
