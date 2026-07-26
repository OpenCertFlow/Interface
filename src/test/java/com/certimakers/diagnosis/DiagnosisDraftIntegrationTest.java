package com.certimakers.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 진단 입력 초안(F-APP-004). 미완성 입력을 저장·조회·수정·삭제할 수 있고, 본인만 접근함을 검증한다.
 * 초안은 진단을 실행하지 않으므로 <b>미완성 입력도 그대로 저장</b>된다는 점이 완성 진단과의 차이다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class DiagnosisDraftIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockitoBean
    JavaMailSender mailSender;

    @Autowired
    WebTestClient client;

    private static final String PASSWORD = "password1234";

    private String signUpAndLogin(String tag) {
        client.post().uri("/api/v1/auth/signup")
                .bodyValue(Map.of("email", tag + "@example.com", "password", PASSWORD, "nickname", tag,
                        "agreedTermKeys", List.of("SERVICE", "PRIVACY")))
                .exchange().expectStatus().isCreated();
        JsonNode login = client.post().uri("/api/v1/auth/login")
                .bodyValue(Map.of("email", tag + "@example.com", "password", PASSWORD))
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        return login.at("/data/accessToken").asText();
    }

    /** 일부러 미완성(제품명만) 입력이다 — 초안은 검증하지 않고 그대로 보존해야 한다. */
    private static Map<String, Object> partialDraft() {
        return Map.of(
                "productGroup", "ELECTRIC_HEATING_PAD",
                "input", Map.of("productName", "작성 중인 전기방석", "usesElectricity", true));
    }

    private String createDraft(String token) {
        JsonNode created = client.post().uri("/api/v1/diagnoses/drafts")
                .header("Authorization", "Bearer " + token)
                .bodyValue(partialDraft())
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        return created.at("/data/id").asText();
    }

    @Test
    @DisplayName("미완성 입력을 초안으로 저장하고 원문 그대로 다시 조회한다")
    void 초안을_저장하고_조회한다() {
        String token = signUpAndLogin("draft-user");
        String id = createDraft(token);

        client.get().uri("/api/v1/diagnoses/drafts/{id}", id)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.productGroup").isEqualTo("ELECTRIC_HEATING_PAD")
                .jsonPath("$.data.input.productName").isEqualTo("작성 중인 전기방석");

        // 목록에도 나온다.
        client.get().uri("/api/v1/diagnoses/drafts")
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.data[0].id").isEqualTo(id);
    }

    @Test
    @DisplayName("초안을 수정하면 입력이 갱신된다")
    void 초안을_수정한다() {
        String token = signUpAndLogin("draft-edit");
        String id = createDraft(token);

        client.put().uri("/api/v1/diagnoses/drafts/{id}", id)
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("productGroup", "ELECTRIC_HEATING_PAD",
                        "input", Map.of("productName", "수정된 방석", "usesElectricity", true)))
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.data.input.productName").isEqualTo("수정된 방석");
    }

    @Test
    @DisplayName("초안을 삭제하면 조회되지 않는다")
    void 초안을_삭제한다() {
        String token = signUpAndLogin("draft-delete");
        String id = createDraft(token);

        client.method(HttpMethod.DELETE).uri("/api/v1/diagnoses/drafts/{id}", id)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isNoContent();

        client.get().uri("/api/v1/diagnoses/drafts/{id}", id)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("비로그인은 초안을 저장할 수 없고, 남의 초안은 조회되지 않는다")
    void 초안은_본인만_접근한다() {
        // 비로그인 저장은 401
        client.post().uri("/api/v1/diagnoses/drafts")
                .bodyValue(partialDraft())
                .exchange().expectStatus().isUnauthorized();

        String owner = signUpAndLogin("draft-owner");
        String other = signUpAndLogin("draft-other");
        String id = createDraft(owner);

        // 남의 초안은 찾을 수 없음
        client.get().uri("/api/v1/diagnoses/drafts/{id}", id)
                .header("Authorization", "Bearer " + other)
                .exchange().expectStatus().isNotFound();
    }
}
