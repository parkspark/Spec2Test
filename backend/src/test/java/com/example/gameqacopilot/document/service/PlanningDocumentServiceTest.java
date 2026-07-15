package com.example.gameqacopilot.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class PlanningDocumentServiceTest {
    @Test
    void persistsUploadProcessingAndReadyStates() throws Exception {
        var documents = mock(PlanningDocumentRepository.class);
        var projects = mock(ProjectRepository.class);
        var users = mock(UserRepository.class);
        var processor = mock(PdfDocumentProcessor.class);
        when(projects.findById(1L)).thenReturn(Optional.of(mock(Project.class)));
        when(users.findById(2L)).thenReturn(Optional.of(mock(User.class)));
        when(processor.process(any(), any())).thenReturn(new PdfDocumentProcessor.ProcessedPdf("text", "[]"));
        List<DocumentProcessingStatus> statuses = new ArrayList<>();
        doAnswer(invocation -> {
            PlanningDocument document = invocation.getArgument(0);
            statuses.add(document.getProcessingStatus());
            return document;
        }).when(documents).save(any());
        var service = new PlanningDocumentService(documents, projects, users, new MultipartProperties(),
                processor, new ObjectMapper(), "build/test-documents");

        service.upload(1L, 2L, "Plan",
                new MockMultipartFile("file", "plan.pdf", "application/pdf", pdf()));

        assertThat(statuses).containsExactly(DocumentProcessingStatus.UPLOADED,
                DocumentProcessingStatus.PROCESSING, DocumentProcessingStatus.READY);
    }

    @Test
    void oversizedPdfIsRejectedAndRecordedAsFailed() {
        var documents = mock(PlanningDocumentRepository.class);
        var projects = mock(ProjectRepository.class);
        var users = mock(UserRepository.class);
        when(projects.findById(1L)).thenReturn(Optional.of(mock(Project.class)));
        when(users.findById(2L)).thenReturn(Optional.of(mock(User.class)));
        var multipart = new MultipartProperties();
        multipart.setMaxFileSize(DataSize.ofBytes(5));
        var service = new PlanningDocumentService(documents, projects, users, multipart,
                mock(PdfDocumentProcessor.class), new ObjectMapper(), "build/test-documents");
        var file = new MockMultipartFile("file", "plan.pdf", "application/pdf", "%PDF-1.7".getBytes());

        assertThatThrownBy(() -> service.upload(1L, 2L, "Plan", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum size");

        var captor = ArgumentCaptor.forClass(PlanningDocument.class);
        verify(documents).save(captor.capture());
        assertThat(captor.getValue().getProcessingStatus()).isEqualTo(DocumentProcessingStatus.FAILED);
        assertThat(captor.getValue().getFailureReason()).contains("maximum size");
    }

    private byte[] pdf() throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}
