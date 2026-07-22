package com.certimakers.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
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
 * 시뮬레이션·보완 계획 엔드투엔드 검증. 실제 DB의 시드 룰셋 위에서 돈다.
 *
 * <p>시드 룰의 구체적인 서류 구성에 의존하지 않도록, 보완 계획이 알려준 서류를 그대로 시뮬레이션
 * 입력으로 넘겨 <b>두 기능이 서로 일관된 숫자를 말하는지</b>를 검증한다. 룰셋이 바뀌어도 이 테스트는
 * 계속 유효하다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class SimulationFlowIntegrationTest {

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

    /** 서류를 하나도 보유하지 않은 220V 드라이기. 보완할 것이 많은 상태에서 시작한다. */
    private String createBareDiagnosis() {
        Map<String, Object> request = Map.of(
                "productName", "가정용 헤어드라이어",
                "productGroup", "SMALL_APPLIANCE",
                "usesElectricity", true,
                "ratedVoltage", 220,
                "powerConsumption", 1200,
                "hasBattery", false,
                "targetUser", "GENERAL",
                "salesChannel", "ONLINE",
                "materials", List.of("PLASTIC", "METAL"),
                "heldDocuments", List.of());

        JsonNode created = webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class)
                .returnResult().getResponseBody();

        return created.at("/data/id").asText();
    }

    @Test
    @DisplayName("보완 계획이 알려준 서류를 시뮬레이션하면 예고한 점수와 정확히 일치한다")
    void 보완_계획과_시뮬레이션이_같은_숫자를_말한다() {
        String id = createBareDiagnosis();

        // ① 100% 목표 보완 계획
        JsonNode plan = webTestClient.get()
                .uri("/api/v1/diagnoses/{id}/remediation-plan?targetScore=100", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class)
                .returnResult().getResponseBody();

        assertThat(plan.at("/data/applicable").asBoolean()).isTrue();
        assertThat(plan.at("/data/achievable").asBoolean()).isTrue();
        assertThat(plan.at("/data/projectedScore").asInt()).isEqualTo(100);

        JsonNode steps = plan.at("/data/steps");
        assertThat(steps).isNotEmpty();

        // ② 첫 단계 서류 하나만 준비했다고 가정하고 시뮬레이션
        JsonNode firstStep = steps.get(0);
        String documentCode = firstStep.at("/documentCode").asText();
        int predictedScore = firstStep.at("/scoreAfter").asInt();

        JsonNode simulated = webTestClient.post()
                .uri("/api/v1/diagnoses/{id}/simulations", id)
                .bodyValue(Map.of("addDocuments", List.of(documentCode)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class)
                .returnResult().getResponseBody();

        // ③ 두 기능이 같은 숫자를 말해야 한다. 어긋나면 사용자에게 거짓말을 하는 것이다.
        assertThat(simulated.at("/data/simulatedScore/percentage").asInt())
                .isEqualTo(predictedScore);
        assertThat(simulated.at("/data/newlySatisfiedDocuments").toString())
                .contains(documentCode);
        assertThat(simulated.at("/data/percentagePointChange").asInt())
                .isEqualTo(firstStep.at("/gainPercentagePoints").asInt());
    }

    @Test
    @DisplayName("어린이용으로 바꾸면 적용 인증 제도가 달라진 것을 알려준다")
    void 사양을_바꾸면_인증_제도_변화를_알려준다() {
        String id = createBareDiagnosis();

        webTestClient.post().uri("/api/v1/diagnoses/{id}/simulations", id)
                .bodyValue(Map.of("targetUser", "CHILD"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.certificationScopeChanged").isEqualTo(true)
                .jsonPath("$.data.addedCandidates").isNotEmpty();
    }

    @Test
    @DisplayName("시뮬레이션은 원본 진단을 덮어쓰지 않는다")
    void 시뮬레이션은_원본을_덮어쓰지_않는다() {
        String id = createBareDiagnosis();

        int before = webTestClient.get().uri("/api/v1/diagnoses/{id}", id)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .at("/data/score/percentage").asInt();

        webTestClient.post().uri("/api/v1/diagnoses/{id}/simulations", id)
                .bodyValue(Map.of("addDocuments", List.of("BIZ_LICENSE", "TEST_REPORT")))
                .exchange().expectStatus().isOk();

        int after = webTestClient.get().uri("/api/v1/diagnoses/{id}", id)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .at("/data/score/percentage").asInt();

        // 원본은 특정 시점의 룰셋으로 확정된 기록이다. 가정이 그것을 바꾸면 안 된다.
        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("목표 준비도가 범위를 벗어나면 400")
    void 목표_준비도_범위를_벗어나면_400() {
        String id = createBareDiagnosis();

        webTestClient.get()
                .uri("/api/v1/diagnoses/{id}/remediation-plan?targetScore=150", id)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("존재하지 않는 진단으로 시뮬레이션하면 404")
    void 존재하지_않는_진단은_404() {
        webTestClient.post()
                .uri("/api/v1/diagnoses/{id}/simulations",
                        "00000000-0000-0000-0000-000000000000")
                .bodyValue(Map.of("addDocuments", List.of("BIZ_LICENSE")))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("CM-DIAG-002");
    }
}
