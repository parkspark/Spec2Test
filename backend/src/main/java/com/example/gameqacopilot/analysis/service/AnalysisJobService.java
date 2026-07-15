package com.example.gameqacopilot.analysis.service;

import com.example.gameqacopilot.analysis.dto.AnalysisJobResponse;
import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import com.example.gameqacopilot.analysis.repository.AnalysisJobRepository;
import com.example.gameqacopilot.document.entity.DocumentProcessingStatus;
import com.example.gameqacopilot.document.repository.PlanningDocumentRepository;
import com.example.gameqacopilot.user.UserRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisJobService {
    private final AnalysisJobRepository jobs;
    private final PlanningDocumentRepository documents;
    private final UserRepository users;

    public AnalysisJobService(AnalysisJobRepository jobs, PlanningDocumentRepository documents, UserRepository users) {
        this.jobs = jobs;
        this.documents = documents;
        this.users = users;
    }

    @Transactional
    public AnalysisJobResponse request(Long documentId, Long userId) {
        var document = documents.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document not found"));
        if (document.getProcessingStatus() != DocumentProcessingStatus.READY) {
            throw new IllegalArgumentException("Document is not ready for analysis");
        }
        var user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return AnalysisJobResponse.from(jobs.save(new AnalysisJob(document, user)));
    }

    @Transactional
    public AnalysisJobResponse findById(Long id) {
        return AnalysisJobResponse.from(jobs.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Analysis not found")));
    }

    @Transactional(readOnly = true)
    public AnalysisJobResponse findLatest(Long documentId) {
        if (!documents.existsById(documentId)) throw new NoSuchElementException("Document not found");
        return AnalysisJobResponse.from(jobs.findFirstByPlanningDocument_IdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new NoSuchElementException("Analysis not found")));
    }
}
