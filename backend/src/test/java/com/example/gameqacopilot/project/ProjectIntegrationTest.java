package com.example.gameqacopilot.project;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.gameqacopilot.common.security.CurrentUser;
import com.example.gameqacopilot.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcClient jdbcClient;
    @Autowired ObjectMapper objectMapper;
    private CurrentUser qa;
    private CurrentUser user;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM analysis_jobs").update();
        jdbcClient.sql("DELETE FROM planning_documents").update();
        jdbcClient.sql("DELETE FROM projects").update();
        jdbcClient.sql("DELETE FROM users").update();
        jdbcClient.sql("INSERT INTO users (email, password, name, role) VALUES (?, ?, ?, ?)")
                .params("qa@example.com", "unused", "QA", "QA").update();
        jdbcClient.sql("INSERT INTO users (email, password, name, role) VALUES (?, ?, ?, ?)")
                .params("user@example.com", "unused", "User", "USER").update();
        qa = principal("qa@example.com", UserRole.QA);
        user = principal("user@example.com", UserRole.USER);
    }

    @Test
    void qaCreatesProjectOwnedByCurrentUserWithActiveStatus() throws Exception {
        var body = Map.of("name", "Arcade QA", "description", "Combat",
                "gameGenre", "Action", "platform", "PC");
        mockMvc.perform(post("/api/projects").with(user(qa))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Arcade QA"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.ownerId").value(qa.id()));

        Long id = jdbcClient.sql("SELECT id FROM projects WHERE name = 'Arcade QA'")
                .query(Long.class).single();
        mockMvc.perform(get("/api/projects").with(user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Arcade QA"));
        mockMvc.perform(get("/api/projects/{id}", id).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platform").value("PC"));
    }

    @Test
    void userCannotCreateAndMissingProjectReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/projects").with(user(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Forbidden"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/999999").with(user(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsProjectWithoutName() throws Exception {
        mockMvc.perform(post("/api/projects").with(user(qa))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
    private CurrentUser principal(String email, UserRole role) {
        Long id = jdbcClient.sql("SELECT id FROM users WHERE email = ?")
                .param(email).query(Long.class).single();
        return new CurrentUser(id, email, "unused", role.name(), role);
    }
}
