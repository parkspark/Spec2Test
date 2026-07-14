package com.example.gameqacopilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
class DatabaseMigrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void createsUserAndProjectTablesWithRequiredConstraints() {
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = TRUE")
                .query(Long.class)
                .single())
                .isEqualTo(1);

        jdbcClient.sql("""
                        INSERT INTO users (email, password, name, role)
                        VALUES ('qa@example.com', 'encoded', 'QA', 'QA')
                        """)
                .update();

        Long ownerId = jdbcClient.sql("SELECT id FROM users WHERE email = 'qa@example.com'")
                .query(Long.class)
                .single();

        jdbcClient.sql("""
                        INSERT INTO projects (owner_id, name, status)
                        VALUES (:ownerId, 'Sample Game', 'ACTIVE')
                        """)
                .param("ownerId", ownerId)
                .update();

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM projects").query(Long.class).single())
                .isEqualTo(1);
        assertThatThrownBy(() -> jdbcClient.sql("""
                                INSERT INTO users (email, password, name, role)
                                VALUES ('invalid@example.com', 'encoded', 'Invalid', 'ADMIN')
                                """)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcClient.sql("""
                                INSERT INTO projects (owner_id, name, status)
                                VALUES (9999, 'Orphan', 'ACTIVE')
                                """)
                        .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}