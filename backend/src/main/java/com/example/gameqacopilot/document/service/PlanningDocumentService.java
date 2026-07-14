package com.example.gameqacopilot.document.service;

import com.example.gameqacopilot.document.dto.PlanningDocumentResponse;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.document.parser.PdfDocumentProcessor;
import com.example.gameqacopilot.document.repository.PlanningDocumentRepository;
import com.example.gameqacopilot.project.ProjectRepository;
import com.example.gameqacopilot.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PlanningDocumentService {
    private static final String PDF_MIME_TYPE = "application/pdf";
    private final PlanningDocumentRepository documents;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final MultipartProperties multipartProperties;
    private final PdfDocumentProcessor processor;
    private final Path storageRoot;

    public PlanningDocumentService(
            PlanningDocumentRepository documents,
            ProjectRepository projects,
            UserRepository users,
            MultipartProperties multipartProperties,
            PdfDocumentProcessor processor,
            @Value("${app.document.storage-path:./data/documents}") String storagePath) {
        this.documents = documents;
        this.projects = projects;
        this.users = users;
        this.multipartProperties = multipartProperties;
        this.processor = processor;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public PlanningDocumentResponse upload(Long projectId, Long userId, String title, MultipartFile file) {
        var project = projects.findById(projectId).orElseThrow(() -> new NoSuchElementException("Project not found"));
        var user = users.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        var document = new PlanningDocument(
                project, user, title, safeFileName(file.getOriginalFilename()),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(), file.getSize());
        try {
            byte[] content = validateAndRead(file);
            int pageCount = validatePdf(content);
            Files.createDirectories(storageRoot);
            Path documentDirectory = storageRoot.resolve(UUID.randomUUID().toString());
            Files.createDirectories(documentDirectory);
            Path storedPath = documentDirectory.resolve("original.pdf");
            Files.write(storedPath, content);
            document.uploaded(storedPath.toString(), pageCount);
            var processed = processor.process(content, documentDirectory);
            document.processed(processed.extractedText(), processed.pageContents());
            return PlanningDocumentResponse.from(documents.save(document));
        } catch (IllegalArgumentException exception) {
            document.failed(exception.getMessage());
            documents.save(document);
            throw exception;
        } catch (IOException exception) {
            String reason = "PDF is damaged or could not be stored";
            document.failed(reason);
            documents.save(document);
            throw new IllegalArgumentException(reason, exception);
        }
    }

    private byte[] validateAndRead(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("PDF file is required");
        if (!PDF_MIME_TYPE.equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }
        if (file.getSize() > multipartProperties.getMaxFileSize().toBytes()) {
            throw new IllegalArgumentException("PDF file exceeds the maximum size");
        }
        byte[] content = file.getBytes();
        if (content.length < 5 || content[0] != '%' || content[1] != 'P' || content[2] != 'D'
                || content[3] != 'F' || content[4] != '-') {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }
        return content;
    }

    private int validatePdf(byte[] content) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(content)) {
            if (pdf.isEncrypted()) throw new IllegalArgumentException("Encrypted PDFs are not allowed");
            return pdf.getNumberOfPages();
        } catch (InvalidPasswordException exception) {
            throw new IllegalArgumentException("Encrypted PDFs are not allowed", exception);
        }
    }

    private String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) return "document.pdf";
        return Path.of(originalName).getFileName().toString();
    }
}

