package com.example.gameqacopilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.gameqacopilot.common.security.CurrentUser;
import com.example.gameqacopilot.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.document.storage-path=build/test-full-flow")
@AutoConfigureMockMvc
class FullFlowIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ChatModel chatModel;
    private CurrentUser qa;
    private Long projectId;

    @BeforeEach
    void setUp() {
        for (String table : List.of("lats_loop_logs", "outputs", "ambiguities", "test_cases",
                "requirements", "analysis_jobs", "planning_documents", "projects", "users")) {
            jdbcClient.sql("DELETE FROM " + table).update();
        }
        jdbcClient.sql("INSERT INTO users (email, password, name, role) VALUES (?, ?, ?, ?)")
                .params("qa-flow@example.com", "unused", "QA", "QA").update();
        Long userId = jdbcClient.sql("SELECT id FROM users WHERE email = 'qa-flow@example.com'")
                .query(Long.class).single();
        qa = new CurrentUser(userId, "qa-flow@example.com", "unused", "QA", UserRole.QA);
        jdbcClient.sql("INSERT INTO projects (owner_id, name, status) VALUES (?, ?, ?)")
                .params(userId, "Full flow", "ACTIVE").update();
        projectId = jdbcClient.sql("SELECT id FROM projects WHERE name = 'Full flow'")
                .query(Long.class).single();
    }

    @Test
    void uploadAnalysisReviewAndCsvFlow() throws Exception {
        var upload = mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId)
                        .file(new MockMultipartFile("file", "plan.pdf", "application/pdf", pdf()))
                        .param("title", "Daily reward plan").with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingStatus").value("READY"))
                .andReturn();
        long documentId = objectMapper.readTree(upload.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        when(chatModel.call(any(Prompt.class))).thenReturn(
                response(classificationJson()), response(requirementJson()),
                response(testCaseJson()), response("{\"ambiguities\":[]}"));
        mockMvc.perform(post("/api/documents/{documentId}/analyses", documentId).with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        long testCaseId = jdbcClient.sql("SELECT id FROM test_cases").query(Long.class).single();
        mockMvc.perform(get("/api/projects/{projectId}/test-cases", projectId).with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].testItem").value("Daily reward is granted once"));
        mockMvc.perform(get("/api/test-cases/{testCaseId}/evidences", testCaseId).with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pageNumber").value(1))
                .andExpect(jsonPath("$.data[0].verificationStatus").value("EXACT"));
        mockMvc.perform(get("/api/documents/{documentId}/pages/1", documentId).with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.elements[0].text").value("Free draw once daily"));

        mockMvc.perform(post("/api/test-cases/{testCaseId}/approve", testCaseId).with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        var output = mockMvc.perform(post("/api/projects/{projectId}/outputs/csv", projectId).with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andReturn();
        long outputId = objectMapper.readTree(output.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        byte[] csv = mockMvc.perform(get("/api/outputs/{outputId}/download", outputId).with(user(qa)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(csv).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(new String(csv, StandardCharsets.UTF_8))
                .contains("No,대분류,중분류,소분류,테스트 항목,사전조건,테스트 스텝,기대결과,기획서 원문 근거,비고")
                .contains("Daily reward is granted once");
        mockMvc.perform(get("/api/outputs/{outputId}/loop-logs", outputId).with(user(qa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].evaluationScore").value(100));
    }

    private ChatResponse response(String json) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }

    private String classificationJson() {
        return """
                {"categoryTree":[{"majorCategory":"Daily reward","middleCategories":[
                {"name":"Grant","minorCategories":["Free draw"]}]}],"evidences":[]}
                """;
    }

    private String requirementJson() {
        return """
                {"requirements":[{"requirementId":"REQ-001","majorCategory":"Daily reward",
                "middleCategory":"Grant","minorCategory":"Free draw","title":"Daily free draw",
                "description":"A free draw is provided once daily","actor":"PLAYER",
                "preconditions":["Logged in"],"trigger":"Select free draw",
                "expectedBehaviors":["Reward is granted"],"evidences":[{"evidenceType":"EXPLICIT",
                "pageNumber":1,"sectionTitle":"Reward","sourceText":"Free draw once daily",
                "sourceElementType":"TEXT","reason":"Direct statement"}]}]}
                """;
    }

    private String testCaseJson() {
        return """
                {"testCases":[{"testCaseId":"TC-001","requirementId":"REQ-001","displayOrder":1,
                "majorCategory":"Daily reward","middleCategory":"Grant","minorCategory":"Free draw",
                "testItem":"Daily reward is granted once","testType":"HAPPY_PATH","priority":"HIGH",
                "preconditions":["Logged in"],"testSteps":[{"stepNumber":1,"action":"Select free draw",
                "expectedResult":"Reward appears"}],"expectedResults":["One reward is granted"],
                "confidence":"HIGH","requiresHumanReview":false,"evidences":[{"evidenceType":"EXPLICIT",
                "pageNumber":1,"sectionTitle":"Reward","sourceText":"Free draw once daily",
                "sourceElementType":"TEXT","reason":"Direct statement"}],"notes":[]}]}
                """;
    }

    private byte[] pdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            try (var stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(72, 720);
                stream.showText("Free draw once daily");
                stream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
