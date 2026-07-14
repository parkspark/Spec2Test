package com.example.gameqacopilot.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.document.entity.DocumentProcessingStatus;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.document.parser.PdfDocumentProcessor;
import com.example.gameqacopilot.document.repository.PlanningDocumentRepository;
import com.example.gameqacopilot.project.Project;
import com.example.gameqacopilot.project.ProjectRepository;
import com.example.gameqacopilot.user.User;
import com.example.gameqacopilot.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class PlanningDocumentServiceTest {
    @Test
    void oversizedPdfIsRejectedAndRecordedAsFailed() {
        var documents = mock(PlanningDocumentRepository.class);
        var projects = mock(ProjectRepository.class);
        var users = mock(UserRepository.class);
        when(projects.findById(1L)).thenReturn(Optional.of(mock(Project.class)));
        when(users.findById(2L)).thenReturn(Optional.of(mock(User.class)));
        var multipart = new MultipartProperties();
        multipart.setMaxFileSize(DataSize.ofBytes(5));
        var service = new PlanningDocumentService(documents, projects, users, multipart, mock(PdfDocumentProcessor.class), "build/test-documents");
        var file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "%PDF-1.7".getBytes());

        assertThatThrownBy(() -> service.upload(1L, 2L, "Plan", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum size");

        var captor = ArgumentCaptor.forClass(PlanningDocument.class);
        verify(documents).save(captor.capture());
        assertThat(captor.getValue().getProcessingStatus()).isEqualTo(DocumentProcessingStatus.FAILED);
        assertThat(captor.getValue().getFailureReason()).contains("maximum size");
    }
}
