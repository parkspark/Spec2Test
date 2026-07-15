package com.example.gameqacopilot.output;

import com.example.gameqacopilot.analysis.loop.LatsLoopLogQueryService;
import com.example.gameqacopilot.analysis.loop.LatsLoopLogResponse;
import com.example.gameqacopilot.common.response.ApiResponse;
import com.example.gameqacopilot.common.security.CurrentUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class OutputController {
    private final CsvOutputService outputs;
    private final LatsLoopLogQueryService loopLogs;

    public OutputController(CsvOutputService outputs, LatsLoopLogQueryService loopLogs) {
        this.outputs = outputs;
        this.loopLogs = loopLogs;
    }

    @PostMapping("/api/projects/{projectId}/outputs/csv")
    ApiResponse<OutputResponse> createCsv(
            @PathVariable Long projectId, @AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.of(outputs.create(projectId, user.id()));
    }

    @PostMapping("/api/projects/{projectId}/outputs/markdown")
    ApiResponse<OutputResponse> createMarkdown(
            @PathVariable Long projectId, @AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.of(outputs.createMarkdown(projectId, user.id()));
    }

    @GetMapping("/api/outputs/{outputId}")
    ApiResponse<OutputResponse> findById(@PathVariable Long outputId) {
        return ApiResponse.of(outputs.findById(outputId));
    }

    @GetMapping("/api/outputs/{outputId}/loop-logs")
    ApiResponse<List<LatsLoopLogResponse>> findLoopLogs(@PathVariable Long outputId) {
        return ApiResponse.of(loopLogs.findByOutputId(outputId));
    }

    @GetMapping("/api/outputs/{outputId}/download")
    ResponseEntity<byte[]> download(@PathVariable Long outputId) {
        var download = outputs.download(outputId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.fileName()).build().toString())
                .body(download.content());
    }
}
