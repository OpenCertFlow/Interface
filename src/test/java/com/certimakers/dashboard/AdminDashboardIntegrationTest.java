package com.certimakers.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.auth.application.port.out.TokenProviderPort;
import com.certimakers.auth.domain.model.AuthProvider;
import com.certimakers.auth.domain.model.Email;
import com.certimakers.auth.domain.model.Nickname;
import com.certimakers.auth.domain.model.Role;
import com.certimakers.auth.domain.model.User;
import com.certimakers.auth.domain.model.UserId;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 관리자 대시보드 통계(F-WADM-001) 통합 검증. 여러 컨텍스트 집계가 한 응답으로 나오는지 본다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AdminDashboardIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    TokenProviderPort tokenProvider;

    private String adminToken() {
        User admin = User.reconstitute(
                UserId.of(com.certimakers.support.TestIds.next()), Email.of("admin@certimakers.local"), null,
                Nickname.of("관리자"), Role.ADMIN, AuthProvider.LOCAL, null, true,
                Instant.parse("2026-08-10T00:00:00Z"));
        return tokenProvider.issue(admin).accessToken();
    }

    @Test
    @DisplayName("관리자가 아니면 대시보드를 조회할 수 없다")
    void 관리자가_아니면_조회할_수_없다() {
        webTestClient.get().uri("/api/v1/admin/dashboard")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("대시보드가 여러 컨텍스트의 집계를 한 응답으로 돌려준다")
    void 대시보드_집계를_돌려준다() {
        webTestClient.post().uri("/api/v1/auth/signup")
                .bodyValue(Map.of("email", "dash@example.com", "password", "password1234", "nickname", "dash",
                        "agreedTermKeys", List.of("SERVICE", "PRIVACY")))
                .exchange()
                .expectStatus().isCreated();

        JsonNode stats = webTestClient.get().uri("/api/v1/admin/dashboard")
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        JsonNode data = stats.at("/data");
        assertThat(data.get("userCount").asLong()).isGreaterThanOrEqualTo(1);
        // 시드된 소형가전 룰셋이 활성이므로 활성 룰셋이 최소 1개다.
        assertThat(data.get("activeRuleSetCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(data.has("diagnosisCount")).isTrue();
        assertThat(data.has("consultingLeadCount")).isTrue();
        assertThat(data.has("auditLogCount")).isTrue();
    }
}
