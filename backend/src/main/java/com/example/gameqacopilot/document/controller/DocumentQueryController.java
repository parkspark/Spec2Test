package com.example.gameqacopilot.document.controller;

import com.example.gameqacopilot.common.response.ApiResponse;
import com.example.gameqacopilot.document.dto.PlanningDocumentPageResponse;
import com.example.gameqacopilot.document.dto.PlanningDocumentResponse;
import com.example.gameqacopilot.document.service.PlanningDocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
public class DocumentQueryController {
    private final PlanningDocumentService documentService;

    public DocumentQueryController(PlanningDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{documentId}")
    ApiResponse<PlanningDocumentResponse> findById(@PathVariable Long documentId) {
        return ApiResponse.of(documentService.findById(documentId));
    }

    @GetMapping("/{documentId}/pages/{pageNumber}")
    ApiResponse<PlanningDocumentPageResponse> findPage(
            @PathVariable Long documentId, @PathVariable int pageNumber) {
        return ApiResponse.of(documentService.findPage(documentId, pageNumber));
    }

    @GetMapping("/{documentId}/pages/{pageNumber}/image")
    ResponseEntity<Resource> findPageImage(@PathVariable Long documentId, @PathVariable int pageNumber) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .body(documentService.findPageImage(documentId, pageNumber));
    }
}
