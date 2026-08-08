package io.opencertflow.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.auth.application.port.out.TokenProviderPort;
import io.opencertflow.auth.domain.model.AuthProvider;
import io.opencertflow.auth.domain.model.Email;
import io.opencertflow.auth.domain.model.Nickname;
import io.opencertflow.auth.domain.model.Role;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.auth.domain.model.UserId;
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

/**
 * 관리자 룰셋 관리 API(F-WADM-009/010) 통합 검증.
 *
 * <p>지금까지 룰은 SQL 시드로만 바꿀 수 있었다. 이 테스트는 그 작업이 실제로 API로 옮겨졌는지를
 * <b>검증 → 초안 저장 → 배포</b> 전 과정으로 확인한다. 특히 활성화가 같은 제품군의 기존 활성본을
 * 자동으로 교체하는지(제품군당 활성 룰셋 하나)를 본다.
 *
 * <p>ADMIN 권한은 실제 토큰 발급기로 관리자 토큰을 만들어 검증한다 — 시큐리티가 토큰의 역할
 * 클레임으로 인가하므로 DB 사용자 없이도 경로 보호를 그대로 통과한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AdminRuleSetIntegrationTest {

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

    private static final String VALID_CONDITION =
            "{\"type\":\"attr\",\"attribute\":\"USES_ELECTRICITY\",\"operator\":\"EQ\",\"value\":true}";
    private static final String VALID_EFFECTS =
            "[{\"type\":\"addLabelingCheck\",\"label\":\"정격전압·소비전력 표시\"}]";

    private String adminToken() {
        User admin = User.reconstitute(
                UserId.of(io.opencertflow.support.TestIds.next()), Email.of("admin@opencertflow.local"), null,
                Nickname.of("관리자"), Role.ADMIN, AuthProvider.LOCAL, null, true, Instant.now());
        return tokenProvider.issue(admin).accessToken();
    }

    private Map<String, Object> rule(String code, String condition, String effects) {
        return Map.of(
                "ruleCode", code, "priority", 10,
                "conditionJson", condition, "effectsJson", effects,
                "description", "테스트 룰");
    }

    @Test
    @DisplayName("관리자 권한이 없으면 룰셋 관리 API에 접근할 수 없다")
    void 관리자가_아니면_접근할_수_없다() {
        webTestClient.get().uri("/api/v1/admin/rule-sets")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("RAG 품질 검증: 관리자가 임의 조건으로 근거 검색 결과를 확인한다 (F-WADM-015)")
    void rag_품질_검증() {
        JsonNode result = post("/api/v1/admin/rag-check",
                Map.of("productGroup", "SMALL_APPLIANCE", "sections", List.of("DOCUMENTS")), 200);
        assertThat(result.at("/data").has("count")).isTrue();
        assertThat(result.at("/data").has("degraded")).isTrue();
        assertThat(result.at("/data").has("evidences")).isTrue();

        webTestClient.post().uri("/api/v1/admin/rag-check")
                .bodyValue(Map.of("productGroup", "SMALL_APPLIANCE"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("잘못된 effects는 검증에서 걸러지고, 올바른 룰은 통과한다")
    void 룰_정의를_검증한다() {
        String badEffects = "[{\"type\":\"nope\"}]";

        JsonNode invalid = post("/api/v1/admin/rule-sets/validate",
                Map.of("rules", List.of(rule("R-X-001", VALID_CONDITION, badEffects))), 200);
        assertThat(invalid.at("/data/valid").asBoolean()).isFalse();
        assertThat(invalid.at("/data/issues").toString()).contains("R-X-001");

        JsonNode valid = post("/api/v1/admin/rule-sets/validate",
                Map.of("rules", List.of(rule("R-X-001", VALID_CONDITION, VALID_EFFECTS))), 200);
        assertThat(valid.at("/data/valid").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("검증 실패한 룰셋은 저장되지 않는다")
    void 검증_실패한_룰셋은_저장하지_않는다() {
        webTestClient.post().uri("/api/v1/admin/rule-sets")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(Map.of(
                        "productGroup", "SMALL_APPLIANCE",
                        "rules", List.of(rule("R-X-001", VALID_CONDITION, "[{\"type\":\"nope\"}]"))))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("초안 저장 → 배포하면 같은 제품군의 기존 활성본이 교체된다")
    void 초안을_저장하고_배포하면_활성본이_교체된다() {
        // 시드된 SMALL_APPLIANCE 활성 룰셋(v1)이 있는 상태에서 새 버전을 만들어 배포한다.
        String created = post("/api/v1/admin/rule-sets",
                Map.of("productGroup", "SMALL_APPLIANCE",
                        "rules", List.of(rule("R-SA-NEW", VALID_CONDITION, VALID_EFFECTS))), 201)
                .at("/data/id").asText();

        // 저장 직후에는 비활성 초안이다.
        JsonNode detail = get("/api/v1/admin/rule-sets/" + created);
        assertThat(detail.at("/data/active").asBoolean()).isFalse();
        assertThat(detail.at("/data/rules").toString()).contains("R-SA-NEW");

        // 배포.
        webTestClient.post().uri("/api/v1/admin/rule-sets/{id}/activate", created)
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk();

        // 새 룰셋만 SMALL_APPLIANCE 활성이어야 한다 — 제품군당 활성 하나.
        JsonNode list = get("/api/v1/admin/rule-sets");
        long activeSmallAppliance = 0;
        for (JsonNode node : list.at("/data")) {
            if (node.get("productGroup").asText().equals("SMALL_APPLIANCE")
                    && node.get("active").asBoolean()) {
                activeSmallAppliance++;
                assertThat(node.get("id").asText()).isEqualTo(created);
            }
        }
        assertThat(activeSmallAppliance).isEqualTo(1);
    }

    // ── helpers ──────────────────────────────────────────────────

    private JsonNode post(String uri, Object body, int expectedStatus) {
        return webTestClient.post().uri(uri)
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    private JsonNode get(String uri) {
        return webTestClient.get().uri(uri)
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }
}
