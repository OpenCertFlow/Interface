package io.opencertflow.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.auth.application.port.out.TokenProviderPort;
import io.opencertflow.auth.domain.model.AuthProvider;
import io.opencertflow.auth.domain.model.Email;
import io.opencertflow.auth.domain.model.Nickname;
import io.opencertflow.auth.domain.model.Role;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.auth.domain.model.UserId;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.HashMap;
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

/**
 * 감사 로그(F-BE-018 기록, F-WADM-018 조회) 통합 검증. 관리자 변경 행위가 자동 기록되고,
 * 조회(GET)는 기록되지 않는지 확인한다. 기록은 응답 이후 비동기라 짧게 폴링한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AuditLogIntegrationTest {

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

    private final String adminUserId = io.opencertflow.support.TestIds.nextString();

    private String adminToken() {
        User admin = User.reconstitute(
                UserId.of(Long.parseLong(adminUserId)), Email.of("admin@opencertflow.local"), null,
                Nickname.of("관리자"), Role.ADMIN, AuthProvider.LOCAL, null, true,
                java.time.Instant.parse("2026-08-10T00:00:00Z"));
        return tokenProvider.issue(admin).accessToken();
    }

    private JsonNode auditLogs() {
        return webTestClient.get().uri("/api/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    @Test
    @DisplayName("관리자가 아니면 감사 로그를 조회할 수 없다")
    void 관리자가_아니면_조회할_수_없다() {
        webTestClient.get().uri("/api/v1/admin/audit-logs")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("관리자 변경 행위가 행위자·경로·상태와 함께 기록된다")
    void 관리자_변경_행위가_기록된다() throws InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("weight", 2);
        webTestClient.put().uri("/api/v1/admin/document-weights/CIRCUIT_DIAGRAM")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        JsonNode recorded = pollFor("document-weights");
        assertThat(recorded).as("변경 행위가 감사 로그에 남아야 한다").isNotNull();
        assertThat(recorded.get("httpMethod").asText()).isEqualTo("PUT");
        assertThat(recorded.get("actor").asText()).isEqualTo(adminUserId);
        assertThat(recorded.get("statusCode").asInt()).isEqualTo(200);
    }

    @Test
    @DisplayName("조회(GET)는 감사 로그에 남지 않는다 — 변경 행위만 기록한다")
    void 조회는_기록되지_않는다() throws InterruptedException {
        // 변경 하나를 남겨 로그가 존재하게 한 뒤, GET 항목이 없음을 확인한다.
        Map<String, Object> body = new HashMap<>();
        body.put("weight", 3);
        webTestClient.put().uri("/api/v1/admin/document-weights/PARTS_LIST")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();
        pollFor("document-weights");

        for (JsonNode entry : auditLogs().at("/data")) {
            assertThat(entry.get("httpMethod").asText())
                    .as("GET 요청은 기록되지 않아야 한다")
                    .isNotEqualTo("GET");
        }
    }

    private JsonNode pollFor(String pathFragment) throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            for (JsonNode entry : auditLogs().at("/data")) {
                if (entry.get("requestPath").asText().contains(pathFragment)) {
                    return entry;
                }
            }
            Thread.sleep(Duration.ofMillis(100).toMillis());
        }
        return null;
    }
}
