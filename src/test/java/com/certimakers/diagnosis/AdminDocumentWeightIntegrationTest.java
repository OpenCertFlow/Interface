package com.certimakers.diagnosis;

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
 * 관리자 준비도 가중치 관리 API(F-WADM-011) 통합 검증. 코드 배포 없이 가중치를 조정할 수 있는지,
 * 잘못된 값·없는 코드를 막는지 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AdminDocumentWeightIntegrationTest {

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
                Nickname.of("관리자"), Role.ADMIN, AuthProvider.LOCAL, null, true, Instant.now());
        return tokenProvider.issue(admin).accessToken();
    }

    private int weightOf(String code) {
        JsonNode list = webTestClient.get().uri("/api/v1/admin/document-weights")
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        for (JsonNode row : list.at("/data")) {
            if (row.get("documentCode").asText().equals(code)) {
                return row.get("weight").asInt();
            }
        }
        throw new AssertionError("가중치 목록에 " + code + " 없음");
    }

    @Test
    @DisplayName("관리자가 아니면 가중치 API에 접근할 수 없다")
    void 관리자가_아니면_접근할_수_없다() {
        webTestClient.get().uri("/api/v1/admin/document-weights")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("가중치를 조정하면 목록에 반영된다")
    void 가중치를_조정하면_반영된다() {
        assertThat(weightOf("CIRCUIT_DIAGRAM")).isEqualTo(1);

        Map<String, Object> body = new HashMap<>();
        body.put("weight", 2);
        body.put("note", "상담 중요도 상향");

        webTestClient.put().uri("/api/v1/admin/document-weights/CIRCUIT_DIAGRAM")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        assertThat(weightOf("CIRCUIT_DIAGRAM")).isEqualTo(2);
    }

    @Test
    @DisplayName("0 이하 가중치는 거부한다")
    void 양수가_아닌_가중치는_거부한다() {
        Map<String, Object> body = new HashMap<>();
        body.put("weight", 0);

        webTestClient.put().uri("/api/v1/admin/document-weights/TEST_REPORT")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("기준표에 없는 서류 코드는 편집할 수 없다")
    void 없는_코드는_편집할_수_없다() {
        Map<String, Object> body = new HashMap<>();
        body.put("weight", 5);

        webTestClient.put().uri("/api/v1/admin/document-weights/NOT_A_DOCUMENT")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
