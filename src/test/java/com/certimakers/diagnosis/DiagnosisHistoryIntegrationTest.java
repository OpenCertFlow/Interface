package com.certimakers.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpMethod;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 진단 이력(F-APP-032/034/035). 로그인 상태로 만든 진단만 '내 이력'에 남고, 재진단·삭제도 본인만
 * 할 수 있음을 검증한다. 진단이 기본적으로 익명이라는 성질(비로그인 진단은 이력에 없음)도 함께 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class DiagnosisHistoryIntegrationTest {

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

    private static Map<String, Object> hairDryer() {
        Map<String, Object> request = new HashMap<>();
        request.put("productName", "가정용 헤어드라이어");
        request.put("productGroup", "SMALL_APPLIANCE");
        request.put("usesElectricity", true);
        request.put("ratedVoltage", 220);
        request.put("powerConsumption", 1200);
        request.put("hasBattery", false);
        request.put("targetUser", "GENERAL");
        request.put("salesChannel", "ONLINE");
        request.put("materials", List.of("PLASTIC", "METAL"));
        request.put("heldDocuments", List.of());
        return request;
    }

    private String diagnose(String token) {
        WebTestClient.RequestBodySpec spec = client.post().uri("/api/v1/diagnoses");
        if (token != null) {
            spec = spec.header("Authorization", "Bearer " + token);
        }
        JsonNode report = spec.bodyValue(hairDryer())
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        return report.at("/data/id").asText();
    }

    private JsonNode listMine(String token) {
        return client.get().uri("/api/v1/diagnoses/mine")
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    @Test
    @DisplayName("로그인 상태로 만든 진단만 내 이력에 남고, 비로그인 진단은 익명이라 이력에 없다")
    void 로그인_진단만_이력에_남는다() {
        String token = signUpAndLogin("history-user");
        String ownedId = diagnose(token);
        String anonymousId = diagnose(null);

        JsonNode mine = listMine(token);
        assertThat(mine.at("/data").toString()).contains(ownedId);
        assertThat(mine.at("/data").toString()).doesNotContain(anonymousId);
    }

    @Test
    @DisplayName("재진단하면 같은 입력으로 새 진단이 생기고 내 이력에 추가된다")
    void 재진단하면_새_진단이_이력에_추가된다() {
        String token = signUpAndLogin("rediagnose-user");
        String originalId = diagnose(token);

        JsonNode result = client.post().uri("/api/v1/diagnoses/{id}/rediagnose", originalId)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        String newId = result.at("/data/id").asText();

        assertThat(newId).isNotEqualTo(originalId);
        String mine = listMine(token).at("/data").toString();
        assertThat(mine).contains(originalId).contains(newId);
    }

    @Test
    @DisplayName("내 진단은 삭제할 수 있고, 삭제 후 이력·조회에서 사라진다")
    void 내_진단을_삭제한다() {
        String token = signUpAndLogin("delete-user");
        String id = diagnose(token);

        client.method(HttpMethod.DELETE).uri("/api/v1/diagnoses/{id}", id)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isNoContent();

        assertThat(listMine(token).at("/data").toString()).doesNotContain(id);
        client.get().uri("/api/v1/diagnoses/{id}", id)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("남의 진단은 삭제할 수 없다 — 소유자가 아니면 찾을 수 없음으로 응답한다")
    void 남의_진단은_삭제할_수_없다() {
        String owner = signUpAndLogin("owner-user");
        String other = signUpAndLogin("other-user");
        String id = diagnose(owner);

        client.method(HttpMethod.DELETE).uri("/api/v1/diagnoses/{id}", id)
                .header("Authorization", "Bearer " + other)
                .exchange().expectStatus().isNotFound();

        // 소유자에게는 여전히 남아 있다.
        assertThat(listMine(owner).at("/data").toString()).contains(id);
    }
}
