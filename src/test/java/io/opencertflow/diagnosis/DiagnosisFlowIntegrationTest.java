package io.opencertflow.diagnosis;

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
 * 엔드투엔드 검증. 실제 PostgreSQL(Testcontainers) 위에서 Flyway 마이그레이션 → DB 룰 로딩(JSON 코덱)
 * → 룰 평가 → 진단 저장 → 조회까지 한 흐름으로 돈다.
 *
 * <p>{@code local} 프로파일로 AI 워커 스텁을 쓰고 영속성은 실제 JPA를 쓴다 — 데모 구성과 동일하다.
 * {@code ddl-auto: validate}라 엔티티 매핑이 Flyway 스키마와 어긋나면 컨텍스트 로딩부터 실패한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class DiagnosisFlowIntegrationTest {

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

    @Test
    @DisplayName("220V 드라이기 진단 → 201, 안전확인 후보 · 준비도 점수 · 근거 · 저장까지 한 흐름")
    void 드라이기_진단_전체흐름() {
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
                "heldDocuments", List.of("TEST_REPORT"));

        // POST — 진단 실행
        JsonNode created = webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class)
                .returnResult().getResponseBody();

        assertReport(created);
        String id = created.at("/data/id").asText();

        // GET — 저장된 리포트 재조회. 저장·로드 왕복이 온전한지 확인.
        JsonNode fetched = webTestClient.get().uri("/api/v1/diagnoses/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class)
                .returnResult().getResponseBody();

        assertReport(fetched);
        assertThat(fetched.at("/data/id").asText()).isEqualTo(id);
    }

    private void assertReport(JsonNode response) {
        assertThat(response.at("/success").asBoolean()).isTrue();
        assertThat(response.at("/traceId").asText()).isNotBlank();

        JsonNode data = response.at("/data");
        // 로컬 스텁은 근거·문장을 정상 반환하므로 저하 없이 COMPLETED
        assertThat(data.at("/status").asText()).isEqualTo("COMPLETED");
        assertThat(data.at("/score/applicable").asBoolean()).isTrue();
        assertThat(data.at("/score/percentage").asInt()).isBetween(0, 100);

        // 안전확인 후보가 식별되고, 그 근거 룰(R-SA-001)이 함께 남는다
        JsonNode candidates = data.at("/candidates");
        assertThat(candidates).isNotEmpty();
        assertThat(candidates.toString()).contains("KC_SAFETY_CONFIRM_ELECTRIC", "R-SA-001");

        // 체크리스트에 필수 서류가 잡히고, 근거(스텁)와 문장이 붙는다
        assertThat(data.at("/checklist").toString()).contains("BIZ_LICENSE");
        assertThat(data.at("/evidences")).isNotEmpty();
        assertThat(data.at("/narration/summary").asText()).isNotBlank();
    }

    @Test
    @DisplayName("전기 미사용 제품 → 후보 없이 NO_MATCHING_RULE 전문가 확인 항목으로 격리")
    void 전기미사용_제품은_전문가확인으로() {
        Map<String, Object> request = Map.of(
                "productName", "수동 빗",
                "productGroup", "SMALL_APPLIANCE",
                "usesElectricity", false,
                "hasBattery", false,
                "targetUser", "GENERAL",
                "salesChannel", "OFFLINE",
                "materials", List.of("PLASTIC"),
                "heldDocuments", List.of());

        webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.candidates").isEmpty()
                .jsonPath("$.data.expertReviewItems[0].reason").isEqualTo("NO_MATCHING_RULE");
    }

    @Test
    @DisplayName("잘못된 제품군 문자열 → 400, 도메인까지 내려가기 전에 검증 오류")
    void 잘못된_enum은_400() {
        Map<String, Object> request = Map.of(
                "productName", "테스트",
                "productGroup", "NOT_A_GROUP",
                "usesElectricity", false,
                "hasBattery", false,
                "targetUser", "GENERAL",
                "salesChannel", "ONLINE",
                "materials", List.of(),
                "heldDocuments", List.of());

        webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("OCF-C-400");
    }
}
