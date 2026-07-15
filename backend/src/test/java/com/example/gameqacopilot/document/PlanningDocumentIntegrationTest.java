package com.example.gameqacopilot.document;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.gameqacopilot.common.security.CurrentUser;
import com.example.gameqacopilot.user.UserRole;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.servlet.multipart.max-file-size=1MB", "app.document.storage-path=build/test-documents"})
@AutoConfigureMockMvc
class PlanningDocumentIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    private CurrentUser qa;
    private CurrentUser user;
    private Long projectId;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM analysis_jobs").update();
        jdbcClient.sql("DELETE FROM planning_documents").update();
        jdbcClient.sql("DELETE FROM projects").update();
        jdbcClient.sql("DELETE FROM users").update();
        jdbcClient.sql("INSERT INTO users (email, password, name, role) VALUES (?, ?, ?, ?)").params("qa@example.com", "unused", "QA", "QA").update();
        jdbcClient.sql("INSERT INTO users (email, password, name, role) VALUES (?, ?, ?, ?)").params("user@example.com", "unused", "User", "USER").update();
        qa = principal("qa@example.com", UserRole.QA);
        user = principal("user@example.com", UserRole.USER);
        jdbcClient.sql("INSERT INTO projects (owner_id, name, status) VALUES (?, ?, ?)").params(qa.id(), "PDF project", "ACTIVE").update();
        projectId = jdbcClient.sql("SELECT id FROM projects WHERE name = 'PDF project'").query(Long.class).single();
    }

    @Test
    void qaUploadsPdfAndExtractedPageContentsAndImageAreStored() throws Exception {
        var file = new MockMultipartFile("file", "plan.pdf", "application/pdf", pdf(false, "Free draw once daily"));
        mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId).file(file).param("title", "Game plan").with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalFileName").value("plan.pdf"))
                .andExpect(jsonPath("$.data.pageCount").value(1))
                .andExpect(jsonPath("$.data.processingStatus").value("READY"));
        String storedPath = jdbcClient.sql("SELECT stored_file_path FROM planning_documents").query(String.class).single();
        org.assertj.core.api.Assertions.assertThat(java.nio.file.Files.exists(java.nio.file.Path.of(storedPath))).isTrue();
        org.assertj.core.api.Assertions.assertThat(java.nio.file.Files.exists(java.nio.file.Path.of(storedPath).resolveSibling("page-1.png"))).isTrue();
        org.assertj.core.api.Assertions.assertThat(jdbcClient.sql("SELECT extracted_text FROM planning_documents").query(String.class).single())
                .contains("Free draw once daily");
        String pageContents = jdbcClient.sql("SELECT page_contents FROM planning_documents").query(String.class).single();
        org.assertj.core.api.Assertions.assertThat(pageContents)
                .contains("\"pageNumber\":1", "\"elementId\":\"PAGE-1-TEXT-01\"", "\"elementType\":\"TEXT\"", "\"boundingBox\"");
    }

    @Test
    void userCanListDocumentAndReadPageTextLocationAndImage() throws Exception {
        var file = new MockMultipartFile("file", "plan.pdf", "application/pdf", pdf(false, "Free draw once daily"));
        mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId).file(file)
                .param("title", "Game plan").with(user(qa))).andExpect(status().isOk());
        Long documentId = jdbcClient.sql("SELECT id FROM planning_documents").query(Long.class).single();

        mockMvc.perform(get("/api/projects/{projectId}/documents", projectId).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(documentId));
        mockMvc.perform(get("/api/documents/{documentId}", documentId).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Game plan"));
        mockMvc.perform(get("/api/documents/{documentId}/pages/1", documentId).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.elements[0].text").value("Free draw once daily"))
                .andExpect(jsonPath("$.data.elements[0].boundingBox.x").isNumber())
                .andExpect(jsonPath("$.data.imageUrl").value("/api/documents/" + documentId + "/pages/1/image"));
        mockMvc.perform(get("/api/documents/{documentId}/pages/1/image", documentId).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentType())
                        .isEqualTo("image/png"));
    }

    @Test
    void pageOutsideDocumentRangeIsNotFound() throws Exception {
        mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId)
                .file(new MockMultipartFile("file", "plan.pdf", "application/pdf", pdf(false, null)))
                .param("title", "Game plan").with(user(qa))).andExpect(status().isOk());
        Long documentId = jdbcClient.sql("SELECT id FROM planning_documents").query(Long.class).single();

        mockMvc.perform(get("/api/documents/{documentId}/pages/2", documentId).with(user(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidEncryptedDamagedOversizedFilesAndPersistsFailures() throws Exception {
        assertRejected(new MockMultipartFile("file", "plan.txt", "text/plain", "text".getBytes()));
        assertRejected(new MockMultipartFile("file", "broken.pdf", "application/pdf", "%PDF-broken".getBytes()));
        assertRejected(new MockMultipartFile("file", "secret.pdf", "application/pdf", pdf(true, null)));
        assertRejected(new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[1024 * 1024 + 1]));
        Integer failures = jdbcClient.sql("SELECT COUNT(*) FROM planning_documents WHERE processing_status = 'FAILED'").query(Integer.class).single();
        org.assertj.core.api.Assertions.assertThat(failures).isEqualTo(4);
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId)
                        .file(new MockMultipartFile("file", "plan.pdf", "application/pdf", pdf(false, null)))
                        .param("title", " ").with(user(qa)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotUploadPdf() throws Exception {
        mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId)
                        .file(new MockMultipartFile("file", "plan.pdf", "application/pdf", pdf(false, null)))
                        .param("title", "Forbidden").with(user(user)))
                .andExpect(status().isForbidden());
    }

    private void assertRejected(MockMultipartFile file) throws Exception {
        mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId).file(file).param("title", "Invalid").with(user(qa)))
                .andExpect(status().isBadRequest());
    }

    private byte[] pdf(boolean encrypted, String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            if (text != null) {
                try (var stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(72, 720);
                    stream.showText(text);
                    stream.endText();
                }
            }
            if (encrypted) document.protect(new StandardProtectionPolicy("owner", "user", new AccessPermission()));
            document.save(output);
            return output.toByteArray();
        }
    }

    private CurrentUser principal(String email, UserRole role) {
        Long id = jdbcClient.sql("SELECT id FROM users WHERE email = ?").param(email).query(Long.class).single();
        return new CurrentUser(id, email, "unused", role.name(), role);
    }
}
