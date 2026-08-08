package io.opencertflow.diagnosis;

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
import org.springframework.http.HttpStatus;
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

        String newId = rediagnose(token, originalId);

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

    /** 서류를 바꾸지 않고 재진단한다 — 앱이 이전 입력을 그대로 다시 제출하는 경우. */
    private String rediagnose(String token, String originalId) {
        return rediagnose(token, originalId, List.of());
    }

    /**
     * 보유 서류를 지정해 재진단하고 새 진단 id를 돌려준다.
     *
     * <p>재진단은 본문이 필수다 — 앱이 {@code GET /{id}/input}으로 채운 폼을 제출하는 구조라,
     * 테스트도 같은 입력을 실어 보낸다.
     */
    private String rediagnose(String token, String originalId, List<String> heldDocuments) {
        Map<String, Object> body = new HashMap<>(hairDryer());
        body.put("heldDocuments", heldDocuments);
        JsonNode result = client.post().uri("/api/v1/diagnoses/{id}/rediagnose", originalId)
                .header("Authorization", "Bearer " + token)
                .bodyValue(body)
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
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().jsonPath("$.error.code").isEqualTo("OCF-DIAG-006");
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

    // ── 재진단 입력 갱신(F-APP-034) ────────────────────────────────

    private JsonNode getInput(String token, String id) {
        return client.get().uri("/api/v1/diagnoses/{id}/input", id)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    @Test
    @DisplayName("이전 입력을 조회하면 진단할 때 보낸 값이 그대로 나와 앱이 폼을 채울 수 있다")
    void 이전_입력을_조회한다() {
        String token = signUpAndLogin("input-user");
        String id = diagnose(token);

        JsonNode input = getInput(token, id).at("/data");

        // diagnose()가 보낸 hairDryer() 본문과 같은 값이어야 한다.
        assertThat(input.at("/productName").asText()).isEqualTo("가정용 헤어드라이어");
        assertThat(input.at("/productGroup").asText()).isEqualTo("SMALL_APPLIANCE");
        assertThat(input.at("/ratedVoltage").asInt()).isEqualTo(220);
        assertThat(input.at("/powerConsumption").asInt()).isEqualTo(1200);
        assertThat(input.at("/usesElectricity").asBoolean()).isTrue();
        assertThat(input.at("/targetUser").asText()).isEqualTo("GENERAL");
        assertThat(input.at("/materials").toString()).contains("PLASTIC").contains("METAL");
    }

    @Test
    @DisplayName("서류를 갖춰 재진단하면 준비도가 오르고 비교에 신규 충족 항목이 잡힌다")
    void 서류를_갖추면_준비도가_오른다() {
        String token = signUpAndLogin("improve-user");
        String originalId = diagnose(token);                                    // 보유 서류 없음
        String newId = rediagnose(token, originalId, List.of("TEST_REPORT"));   // 시험성적서 확보

        JsonNode body = client.get().uri("/api/v1/diagnoses/{id}/compare", newId)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        // 입력을 갱신할 통로가 없던 때는 이 값이 항상 0·빈 배열이었다(F-APP-034 미완).
        assertThat(body.at("/data/percentagePointChange").asInt()).isPositive();
        assertThat(body.at("/data/newlyHeldDocuments").toString()).contains("TEST_REPORT");
        assertThat(body.at("/data/stillMissingDocuments").toString())
                .doesNotContain("TEST_REPORT");
        // 룰셋·가중치는 그대로이므로 점수 상승은 온전히 서류 확보 덕이다.
        assertThat(body.at("/data/baselineDiffers").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("재진단에서 제품군을 바꾸면 409로 거부한다 — 다른 제품군은 새 진단이어야 한다")
    void 제품군을_바꾸면_재진단할_수_없다() {
        String token = signUpAndLogin("group-change-user");
        String originalId = diagnose(token);

        Map<String, Object> body = new HashMap<>(hairDryer());
        body.put("productGroup", "ELECTRIC_HEATING_PAD");

        client.post().uri("/api/v1/diagnoses/{id}/rediagnose", originalId)
                .header("Authorization", "Bearer " + token)
                .bodyValue(body)
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().jsonPath("$.error.code").isEqualTo("OCF-DIAG-007");
    }

    @Test
    @DisplayName("남의 진단 입력은 존재를 감춰 404로 응답한다")
    void 남의_입력은_조회할_수_없다() {
        String owner = signUpAndLogin("input-owner");
        String stranger = signUpAndLogin("input-stranger");
        String id = diagnose(owner);

        client.get().uri("/api/v1/diagnoses/{id}/input", id)
                .header("Authorization", "Bearer " + stranger)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("비로그인은 입력을 조회할 수 없다 — SecurityConfig가 permitAll보다 먼저 매칭된다")
    void 비로그인은_입력을_조회할_수_없다() {
        String token = signUpAndLogin("input-anon-user");
        String id = diagnose(token);

        client.get().uri("/api/v1/diagnoses/{id}/input", id)
                .exchange().expectStatus().isUnauthorized();
    }
}
