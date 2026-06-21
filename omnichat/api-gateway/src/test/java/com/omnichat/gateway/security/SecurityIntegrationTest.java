package com.omnichat.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
class SecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testPublicEndpointWithoutToken_ShouldReturnOk() {
        // actuator/health is public
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testProtectedEndpointWithoutToken_ShouldReturnUnauthorized() {
        // any other endpoint is protected
        webTestClient.get().uri("/api/v1/conversations")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Invalid JWT token");
    }

    @Test
    void testProtectedEndpointWithInvalidToken_ShouldReturnUnauthorized() {
        webTestClient.get().uri("/api/v1/conversations")
                .header("Authorization", "Bearer invalid-token-string")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Invalid JWT token");
    }
}
