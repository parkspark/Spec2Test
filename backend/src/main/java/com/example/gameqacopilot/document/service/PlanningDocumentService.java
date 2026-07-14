package com.example.gameqacopilot.document.service;

import com.example.gameqacopilot.document.dto.PlanningDocumentPageResponse;
import com.example.gameqacopilot.document.dto.PlanningDocumentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.gameqacopilot.document.entity.PlanningDocument;
import com.example.gameqacopilot.document.parser.PdfDocumentProcessor;
import com.example.gameqacopilot.document.repository.PlanningDocumentRepository;
import com.example.gameqacopilot.project.ProjectRepository;
import com.example.gameqacopilot.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PlanningDocumentService {
    private static final String PDF_MIME_TYPE = "application/pdf";
    private final PlanningDocumentRepository documents;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final MultipartProperties multipartProperties;
    private final PdfDocumentProcessor processor;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public PlanningDocumentService(
            PlanningDocumentRepository documents,
            ProjectRepository projects,
            UserRepository users,
            MultipartProperties multipartProperties,
            PdfDocumentProcessor processor,
            ObjectMapper objectMapper,
            @Value("${app.document.storage-path:./data/documents}") String storagePath) {
        this.documents = documents;
        this.projects = projects;
        this.users = users;
        this.multipartProperties = multipartProperties;
        this.processor = processor;
        this.objectMapper = objectMapper;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<PlanningDocumentResponse> findAll(Long projectId) {
        if (!projects.existsById(projectId)) throw new NoSuchElementException("Project not found");
        return documents.findAllByProject_IdOrderByCreatedAtDesc(projectId).stream()
                .map(PlanningDocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlanningDocumentResponse findById(Long documentId) {
        return PlanningDocumentResponse.from(requireDocument(documentId));
    }

    @Transactional(readOnly = true)
    public PlanningDocumentPageResponse findPage(Long documentId, int pageNumber) {
        PlanningDocument document = requirePage(documentId, pageNumber);
        try {
            var pages = Arrays.asList(objectMapper.readValue(
                    document.getPageContents(), PlanningDocumentPageResponse[].class));
            var page = pages.stream().filter(item -> item.pageNumber() == pageNumber).findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Document page not found"));
            return new PlanningDocumentPageResponse(page.pageNumber(), page.elements(),
                    "/api/documents/%d/pages/%d/image".formatted(documentId, pageNumber));
        } catch (IOException exception) {
            throw new IllegalStateException("Stored document page data is invalid", exception);
        }
    }

    @Transactional(readOnly = true)
    public Resource findPageImage(Long documentId, int pageNumber) {
        PlanningDocument document = requirePage(documentId, pageNumber);
        var image = new FileSystemResource(Path.of(document.getStoredFilePath()).resolveSibling("page-" + pageNumber + ".png"));
        if (!image.exists()) throw new NoSuchElementException("Document page image not found");
        return image;
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

    private PlanningDocument requireDocument(Long documentId) {
        return documents.findById(documentId).orElseThrow(() -> new NoSuchElementException("Document not found"));
    }

    private PlanningDocument requirePage(Long documentId, int pageNumber) {
        PlanningDocument document = requireDocument(documentId);
        if (pageNumber < 1 || document.getPageCount() == null || pageNumber > document.getPageCount()) {
            throw new NoSuchElementException("Document page not found");
        }
        return document;
    }

    private String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) return "document.pdf";
        return Path.of(originalName).getFileName().toString();
    }
}

