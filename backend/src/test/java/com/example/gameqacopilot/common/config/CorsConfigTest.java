package com.example.gameqacopilot.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigTest {

    @Test
    void allowsConfiguredFrontendOriginAndRequiredMethods() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/projects");
        CorsConfiguration configuration = new CorsConfig("http://localhost:5173")
                .corsConfigurationSource()
                .getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(configuration.getAllowedMethods())
                .containsExactlyElementsOf(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}
