package com.example.gameqacopilot.analysis;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;

import com.example.gameqacopilot.common.security.CurrentUser;
import com.example.gameqacopilot.analysis.service.CategoryClassificationService;
import com.example.gameqacopilot.analysis.dto.CategoryClassificationResponse;
import com.example.gameqacopilot.requirement.RequirementExtractionService;
import com.example.gameqacopilot.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisJobIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @MockitoBean CategoryClassificationService classifications;
    @MockitoBean RequirementExtractionService requirements;
    private CurrentUser qa;
    private CurrentUser regularUser;
    private Long documentId;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM analysis_jobs").update();
        jdbcClient.sql("DELETE FROM planning_documents").update();
        jdbcClient.sql("DELETE FROM projects").update();
        jdbcClient.sql("DELETE FROM users").update();
        jdbcClient.sql("INSERT INTO users (email, password, name, role) VALUES (?, ?, ?, ?)")
                .params("qa-analysis@example.com", "unused", "QA", "QA").update();
        jdbcClient.sql("INSERT INTO users (email, password, name, role) VALUES (?, ?, ?, ?)")
                .params("user-analysis@example.com", "unused", "User", "USER").update();
        qa = principal("qa-analysis@example.com", UserRole.QA);
        regularUser = principal("user-analysis@example.com", UserRole.USER);
        jdbcClient.sql("INSERT INTO projects (owner_id, name, status) VALUES (?, ?, ?)")
                .params(qa.id(), "Analysis project", "ACTIVE").update();
        Long projectId = jdbcClient.sql("SELECT id FROM projects WHERE name = 'Analysis project'")
                .query(Long.class).single();
        jdbcClient.sql("""
                INSERT INTO planning_documents
                    (project_id, title, original_file_name, mime_type, file_size, page_count,
                     processing_status, created_by)
                VALUES (?, 'Ready plan', 'plan.pdf', 'application/pdf', 10, 1, 'READY', ?)
                """).params(projectId, qa.id()).update();
        documentId = jdbcClient.sql("SELECT id FROM planning_documents WHERE title = 'Ready plan'")
                .query(Long.class).single();
    }

    @Test
    void qaRequestsAnalysisAndAuthenticatedUsersCanReadIt() throws Exception {
        var classification = new CategoryClassificationResponse(java.util.List.of(), java.util.List.of());
        org.mockito.Mockito.when(classifications.classify(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(classification);
        mockMvc.perform(post("/api/documents/{documentId}/analyses", documentId).with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.planningDocumentId").value(documentId));
        Long analysisId = jdbcClient.sql("SELECT id FROM analysis_jobs").query(Long.class).single();
        verify(classifications).classify(analysisId);
        verify(requirements).extract(analysisId, classification.categoryTree());

        mockMvc.perform(get("/api/analyses/{analysisId}", analysisId).with(user(regularUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedBy").value(qa.id()));
        mockMvc.perform(get("/api/documents/{documentId}/analyses/latest", documentId)
                        .with(user(regularUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(analysisId));
    }

    @Test
    void userCannotRequestAnalysis() throws Exception {
        mockMvc.perform(post("/api/documents/{documentId}/analyses", documentId).with(user(regularUser)))
                .andExpect(status().isForbidden());
    }

    private CurrentUser principal(String email, UserRole role) {
        Long id = jdbcClient.sql("SELECT id FROM users WHERE email = ?").param(email)
                .query(Long.class).single();
        return new CurrentUser(id, email, "unused", role.name(), role);
    }
}
