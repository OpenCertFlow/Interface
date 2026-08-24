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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 인증 준비 트래커(F-APP-049). 진단의 누락 서류로 목록을 만들고 체크해 진행률이 오르는 흐름과,
 * 본인 소유 진단만 다룰 수 있음을 검증한다. 진단 이력 목록에 진행률이 실려 나가는 것도 함께 본다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class PrepPlanIntegrationTest {

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

    // ── 픽스처 ────────────────────────────────────────────────────

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

    /** 보유 서류가 없어 누락이 여러 건 나오는 입력. 트래커에 담을 항목이 생긴다. */
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

    private JsonNode createOrGetPlan(String token, String diagnosisId) {
        return client.put().uri("/api/v1/me/prep-plans/{id}", diagnosisId)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    private JsonNode check(String token, String diagnosisId, String documentCode, boolean done) {
        return client.patch().uri("/api/v1/me/prep-plans/{id}/items/{code}", diagnosisId, documentCode)
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("done", done))
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    /** 목록 첫 항목의 서류 코드. 룰이 바뀌어도 테스트가 깨지지 않도록 응답에서 꺼내 쓴다. */
    private static String firstCode(JsonNode plan) {
        return plan.at("/data/items/0/documentCode").asText();
    }

    // ── 흐름 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("진단의 누락 서류로 준비목록이 만들어지고, 처음에는 아무것도 완료되지 않았다")
    void 진단의_누락_서류로_목록이_만들어진다() {
        String token = signUpAndLogin("prep-create");
        String diagnosisId = diagnose(token);

        JsonNode plan = createOrGetPlan(token, diagnosisId);

        assertThat(plan.at("/data/diagnosisId").asText()).isEqualTo(diagnosisId);
        assertThat(plan.at("/data/total").asInt()).isPositive();
        assertThat(plan.at("/data/completed").asInt()).isZero();
        assertThat(plan.at("/data/progress").asInt()).isZero();
        assertThat(plan.at("/data/hasItems").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("여러 번 불러도 목록은 하나이고 체크 상태가 유지된다 (PUT은 멱등)")
    void 여러_번_불러도_목록은_하나다() {
        String token = signUpAndLogin("prep-idempotent");
        String diagnosisId = diagnose(token);
        JsonNode first = createOrGetPlan(token, diagnosisId);
        check(token, diagnosisId, firstCode(first), true);

        JsonNode again = createOrGetPlan(token, diagnosisId);

        assertThat(again.at("/data/total").asInt()).isEqualTo(first.at("/data/total").asInt());
        assertThat(again.at("/data/completed").asInt()).isEqualTo(1);   // 새로 만들어지지 않았다
    }

    @Test
    @DisplayName("항목을 체크하면 진행률이 오르고, 해제하면 다시 내려간다")
    void 체크하면_진행률이_움직인다() {
        String token = signUpAndLogin("prep-check");
        String diagnosisId = diagnose(token);
        String code = firstCode(createOrGetPlan(token, diagnosisId));

        JsonNode checked = check(token, diagnosisId, code, true);
        assertThat(checked.at("/data/completed").asInt()).isEqualTo(1);
        assertThat(checked.at("/data/progress").asInt()).isPositive();

        JsonNode unchecked = check(token, diagnosisId, code, false);
        assertThat(unchecked.at("/data/completed").asInt()).isZero();
        assertThat(unchecked.at("/data/progress").asInt()).isZero();
    }

    @Test
    @DisplayName("체크 상태는 저장되어 다시 조회해도 남아 있다")
    void 체크_상태가_저장된다() {
        String token = signUpAndLogin("prep-persist");
        String diagnosisId = diagnose(token);
        String code = firstCode(createOrGetPlan(token, diagnosisId));
        check(token, diagnosisId, code, true);

        JsonNode fetched = client.get().uri("/api/v1/me/prep-plans/{id}", diagnosisId)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        assertThat(fetched.at("/data/completed").asInt()).isEqualTo(1);
        assertThat(fetched.at("/data/items/0/done").asBoolean()).isTrue();
    }

    // ── 거부 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("목록에 없는 서류 코드는 거부한다 — 임의 코드로 목록을 늘리지 못한다")
    void 목록에_없는_코드는_거부한다() {
        String token = signUpAndLogin("prep-unknown-code");
        String diagnosisId = diagnose(token);
        createOrGetPlan(token, diagnosisId);

        client.patch().uri("/api/v1/me/prep-plans/{id}/items/{code}", diagnosisId, "NOT_A_DOCUMENT")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("done", true))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("남의 진단으로는 목록을 만들 수 없다 — 존재를 드러내지 않도록 404")
    void 남의_진단은_트래커를_만들_수_없다() {
        String owner = signUpAndLogin("prep-owner");
        String stranger = signUpAndLogin("prep-stranger");
        String diagnosisId = diagnose(owner);

        client.put().uri("/api/v1/me/prep-plans/{id}", diagnosisId)
                .header("Authorization", "Bearer " + stranger)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("익명 진단은 소유자가 없어 트래커를 만들 수 없다")
    void 익명_진단은_트래커를_만들_수_없다() {
        String token = signUpAndLogin("prep-anonymous");
        String anonymousId = diagnose(null);

        client.put().uri("/api/v1/me/prep-plans/{id}", anonymousId)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("아직 만들지 않은 목록을 조회하면 404다 — GET은 만들지 않는다")
    void 조회는_목록을_만들지_않는다() {
        String token = signUpAndLogin("prep-get-only");
        String diagnosisId = diagnose(token);

        client.get().uri("/api/v1/me/prep-plans/{id}", diagnosisId)
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @DisplayName("인증 없이는 트래커에 접근할 수 없다")
    void 비로그인은_접근할_수_없다() {
        String token = signUpAndLogin("prep-no-auth");
        String diagnosisId = diagnose(token);

        client.put().uri("/api/v1/me/prep-plans/{id}", diagnosisId)
                .exchange().expectStatus().isUnauthorized();
    }

    // ── 진단 이력 목록 노출 ────────────────────────────────────────

    @Test
    @DisplayName("진단 이력 목록에 준비 진행률이 함께 오고, 트래커가 없는 진단은 필드 자체가 없다")
    void 이력_목록에_진행률이_실린다() {
        String token = signUpAndLogin("prep-history");
        String tracked = diagnose(token);
        String untracked = diagnose(token);
        String code = firstCode(createOrGetPlan(token, tracked));
        check(token, tracked, code, true);

        JsonNode mine = client.get().uri("/api/v1/diagnoses/mine")
                .header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        JsonNode trackedRow = findById(mine, tracked);
        assertThat(trackedRow.get("prepCompleted").asInt()).isEqualTo(1);
        assertThat(trackedRow.get("prepTotal").asInt()).isPositive();
        assertThat(trackedRow.get("prepProgress").asInt()).isPositive();

        // 트래커를 만들지 않은 진단은 null이 아니라 키가 아예 없다(non_null 직렬화).
        JsonNode untrackedRow = findById(mine, untracked);
        assertThat(untrackedRow.hasNonNull("prepCompleted")).isFalse();
        assertThat(untrackedRow.has("prepProgress")).isFalse();
    }

    private JsonNode findById(JsonNode listResponse, String id) {
        for (JsonNode row : listResponse.at("/data")) {
            if (id.equals(row.at("/id").asText())) {
                return row;
            }
        }
        throw new AssertionError("목록에 진단 " + id + "이(가) 없습니다.");
    }
}
