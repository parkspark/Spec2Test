package com.example.gameqacopilot.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.gameqacopilot.analysis.entity.AnalysisJob;
import com.example.gameqacopilot.analysis.entity.AnalysisJobStatus;
import com.example.gameqacopilot.analysis.repository.AnalysisJobRepository;
import com.example.gameqacopilot.document.entity.DocumentProcessingStatus;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.document.repository.PlanningDocumentRepository;
import com.example.gameqacopilot.user.User;
import com.example.gameqacopilot.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalysisJobServiceTest {
    private final AnalysisJobRepository jobs = mock(AnalysisJobRepository.class);
    private final PlanningDocumentRepository documents = mock(PlanningDocumentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AnalysisJobService service = new AnalysisJobService(jobs, documents, users);

    @Test
    void readyDocumentCreatesPendingAnalysis() {
        var document = mock(PlanningDocument.class);
        var user = mock(User.class);
        when(document.getId()).thenReturn(7L);
        when(document.getProcessingStatus()).thenReturn(DocumentProcessingStatus.READY);
        when(user.getId()).thenReturn(3L);
        when(documents.findById(7L)).thenReturn(Optional.of(document));
        when(users.findById(3L)).thenReturn(Optional.of(user));
        when(jobs.save(any(AnalysisJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.request(7L, 3L);

        assertThat(response.planningDocumentId()).isEqualTo(7L);
        assertThat(response.requestedBy()).isEqualTo(3L);
        assertThat(response.status()).isEqualTo(AnalysisJobStatus.PENDING);
        assertThat(response.requestedAt()).isNotNull();
    }
}
