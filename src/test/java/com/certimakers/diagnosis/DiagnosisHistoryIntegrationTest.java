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
import org.testcontainers.containers.GenericContainer;
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

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
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

    // ── 재진단 비교(F-APP-048) ────────────────────────────────────

    /** 재진단을 실행하고 새 진단 id를 돌려준다. */
    private String rediagnose(String token, String originalId) {
        JsonNode result = client.post().uri("/api/v1/diagnoses/{id}/rediagnose", originalId)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        return result.at("/data/id").asText();
    }

    @Test
    @DisplayName("재진단 결과를 원 진단과 비교하면 두 진단 id와 준비도 변화가 나온다")
    void 재진단_결과를_원_진단과_비교한다() {
        String token = signUpAndLogin("compare-user");
        String originalId = diagnose(token);
        String newId = rediagnose(token, originalId);

        JsonNode body = client.get().uri("/api/v1/diagnoses/{id}/compare", newId)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        // 부모를 DB에 저장하고 다시 읽어와야 여기까지 온다 — 영속성 매핑(toEntity/toDomain) 검증.
        assertThat(body.at("/data/previousDiagnosisId").asText()).isEqualTo(originalId);
        assertThat(body.at("/data/diagnosisId").asText()).isEqualTo(newId);
        assertThat(body.at("/data/comparable").asBoolean()).isTrue();
        // 재진단은 원본 입력을 그대로 복사하므로 같은 룰셋에서는 점수가 변하지 않는다.
        assertThat(body.at("/data/percentagePointChange").asInt()).isZero();
        assertThat(body.at("/data/baselineDiffers").asBoolean()).isFalse();
        assertThat(body.at("/data/notice").asText()).contains("사전 점검 지표");
    }

    @Test
    @DisplayName("최초 진단은 비교할 원 진단이 없어 409로 거부한다")
    void 최초_진단은_비교할_수_없다() {
        String token = signUpAndLogin("compare-first-user");
        String originalId = diagnose(token);

        client.get().uri("/api/v1/diagnoses/{id}/compare", originalId)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isEqualTo(org.springframework.http.HttpStatus.CONFLICT)
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-DIAG-006");
    }

    @Test
    @DisplayName("남의 재진단은 존재를 감춰 404로 응답한다")
    void 남의_재진단은_비교할_수_없다() {
        String owner = signUpAndLogin("compare-owner");
        String stranger = signUpAndLogin("compare-stranger");
        String newId = rediagnose(owner, diagnose(owner));

        client.get().uri("/api/v1/diagnoses/{id}/compare", newId)
                .header("Authorization", "Bearer " + stranger)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("비로그인은 비교할 수 없다 — SecurityConfig가 permitAll보다 먼저 매칭된다")
    void 비로그인은_비교할_수_없다() {
        String token = signUpAndLogin("compare-anon-user");
        String newId = rediagnose(token, diagnose(token));

        client.get().uri("/api/v1/diagnoses/{id}/compare", newId)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("이력 목록에 원 진단 id가 함께 나와 앱이 비교 진입점을 그릴 수 있다")
    void 이력_목록에_원_진단_id가_나온다() {
        String token = signUpAndLogin("summary-parent-user");
        String originalId = diagnose(token);
        String newId = rediagnose(token, originalId);

        JsonNode mine = listMine(token).at("/data");

        JsonNode rediagnosis = findById(mine, newId);
        JsonNode original = findById(mine, originalId);
        assertThat(rediagnosis.at("/previousDiagnosisId").asText()).isEqualTo(originalId);
        // 최초 진단은 부모가 없다. default-property-inclusion=non_null이라 필드가 아예 빠지므로
        // "null 값"이 아니라 "키 부재"로 나간다 — 앱은 부재를 최초 진단으로 읽어야 한다.
        assertThat(original.hasNonNull("previousDiagnosisId")).isFalse();
    }

    private JsonNode findById(JsonNode list, String id) {
        for (JsonNode item : list) {
            if (id.equals(item.at("/id").asText())) {
                return item;
            }
        }
        throw new AssertionError("이력 목록에 진단 " + id + "이(가) 없습니다.");
    }
}
