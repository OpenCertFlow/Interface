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
import java.util.UUID;
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
 * 관리자 공식 문서 메타데이터 관리 API(F-WADM-012/013) 통합 검증. 특히 <b>출처 URL 필수</b>(불변식 6)와
 * 알려진 제품군만 허용하는지를 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AdminOfficialDocumentIntegrationTest {

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
                UserId.of(UUID.randomUUID()), Email.of("admin@certimakers.local"), null,
                Nickname.of("관리자"), Role.ADMIN, AuthProvider.LOCAL, null, true, Instant.now());
        return tokenProvider.issue(admin).accessToken();
    }

    private Map<String, Object> validDocument() {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "전기용품 안전확인 대상 및 절차");
        body.put("issuer", "국가기술표준원");
        body.put("publishedAt", "2026-01-31");
        body.put("productGroup", "SMALL_APPLIANCE");
        body.put("certificationType", "SAFETY_CONFIRM");
        body.put("sourceUrl", "https://www.safetykorea.kr/example");
        return body;
    }

    @Test
    @DisplayName("관리자가 아니면 문서 관리 API에 접근할 수 없다")
    void 관리자가_아니면_접근할_수_없다() {
        webTestClient.get().uri("/api/v1/admin/official-documents")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("문서를 등록하면 목록에서 조회된다")
    void 문서를_등록하면_조회된다() {
        String id = webTestClient.post().uri("/api/v1/admin/official-documents")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(validDocument())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .at("/data/id").asText();

        JsonNode fetched = webTestClient.get().uri("/api/v1/admin/official-documents/{id}", id)
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        assertThat(fetched.at("/data/issuer").asText()).isEqualTo("국가기술표준원");
        assertThat(fetched.at("/data/sourceUrl").asText()).contains("safetykorea.kr");
    }

    @Test
    @DisplayName("출처 URL이 없으면 등록할 수 없다 — 출처 없는 문서는 근거가 아니다")
    void 출처가_없으면_등록할_수_없다() {
        Map<String, Object> noSource = validDocument();
        noSource.remove("sourceUrl");

        webTestClient.post().uri("/api/v1/admin/official-documents")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(noSource)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("http(s)가 아닌 출처 URL은 거부한다")
    void 잘못된_출처_URL은_거부한다() {
        Map<String, Object> badUrl = validDocument();
        badUrl.put("sourceUrl", "ftp://example.com/doc");

        webTestClient.post().uri("/api/v1/admin/official-documents")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(badUrl)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("알 수 없는 제품군은 거부한다")
    void 잘못된_제품군은_거부한다() {
        Map<String, Object> badGroup = validDocument();
        badGroup.put("productGroup", "NOT_A_GROUP");

        webTestClient.post().uri("/api/v1/admin/official-documents")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(badGroup)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("문서를 수정하면 반영된다")
    void 문서를_수정하면_반영된다() {
        String id = webTestClient.post().uri("/api/v1/admin/official-documents")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(validDocument())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .at("/data/id").asText();

        Map<String, Object> updated = validDocument();
        updated.put("title", "개정판 제목");

        webTestClient.put().uri("/api/v1/admin/official-documents/{id}", id)
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(updated)
                .exchange()
                .expectStatus().isOk();

        JsonNode fetched = webTestClient.get().uri("/api/v1/admin/official-documents/{id}", id)
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        assertThat(fetched.at("/data/title").asText()).isEqualTo("개정판 제목");
    }
}
