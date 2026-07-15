package com.example.gameqacopilot.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
class OutputMigrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void cleanUp() {
        jdbcClient.sql("DELETE FROM lats_loop_logs").update();
        jdbcClient.sql("DELETE FROM outputs").update();
        jdbcClient.sql("DELETE FROM planning_documents WHERE project_id IN (SELECT id FROM projects WHERE name = 'Output Test')").update();
        jdbcClient.sql("DELETE FROM projects WHERE name = 'Output Test'").update();
        jdbcClient.sql("DELETE FROM users WHERE email = 'output@example.com'").update();
    }

    @Test
    void createsOutputsAndLoopLogsWithRequiredConstraints() {
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '7' AND success = TRUE")
                .query(Long.class).single()).isEqualTo(1);

        jdbcClient.sql("INSERT INTO users (email, password, name, role) VALUES ('output@example.com', 'encoded', 'QA', 'QA')")
                .update();
        Long userId = jdbcClient.sql("SELECT id FROM users WHERE email = 'output@example.com'")
                .query(Long.class).single();
        jdbcClient.sql("INSERT INTO projects (owner_id, name, status) VALUES (:userId, 'Output Test', 'ACTIVE')")
                .param("userId", userId).update();
        Long projectId = jdbcClient.sql("SELECT id FROM projects WHERE name = 'Output Test'")
                .query(Long.class).single();
        jdbcClient.sql("""
                INSERT INTO planning_documents
                    (project_id, title, original_file_name, mime_type, file_size, processing_status, created_by)
                VALUES (:projectId, 'Plan', 'plan.pdf', 'application/pdf', 1, 'READY', :userId)
                """).param("projectId", projectId).param("userId", userId).update();
        Long documentId = jdbcClient.sql("SELECT id FROM planning_documents WHERE project_id = :projectId")
                .param("projectId", projectId).query(Long.class).single();

        jdbcClient.sql("""
                INSERT INTO outputs
                    (project_id, planning_document_id, output_type, status, external_service, request_data, created_by, created_at)
                VALUES (:projectId, :documentId, 'CSV_EXPORT', 'PENDING', 'NONE', '{}', :userId, CURRENT_TIMESTAMP)
                """).param("projectId", projectId).param("documentId", documentId).param("userId", userId).update();
        Long outputId = jdbcClient.sql("SELECT id FROM outputs WHERE project_id = :projectId")
                .param("projectId", projectId).query(Long.class).single();
        jdbcClient.sql("""
                INSERT INTO lats_loop_logs
                    (output_id, depth_step, generated_draft, evaluation_score, evaluation_feedback, created_at)
                VALUES (:outputId, 1, 'draft', 65, 'revise', CURRENT_TIMESTAMP)
                """).param("outputId", outputId).update();

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM lats_loop_logs WHERE output_id = :outputId")
                .param("outputId", outputId).query(Long.class).single()).isEqualTo(1);
        assertThatThrownBy(() -> jdbcClient.sql("""
                INSERT INTO outputs
                    (project_id, planning_document_id, output_type, status, external_service, created_by, created_at)
                VALUES (:projectId, :documentId, 'XLSX_EXPORT', 'PENDING', 'NONE', :userId, CURRENT_TIMESTAMP)
                """).param("projectId", projectId).param("documentId", documentId).param("userId", userId).update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcClient.sql("""
                INSERT INTO lats_loop_logs
                    (output_id, depth_step, generated_draft, evaluation_score, evaluation_feedback, created_at)
                VALUES (999999, 1, 'draft', 65, 'feedback', CURRENT_TIMESTAMP)
                """).update()).isInstanceOf(DataIntegrityViolationException.class);
    }
}
