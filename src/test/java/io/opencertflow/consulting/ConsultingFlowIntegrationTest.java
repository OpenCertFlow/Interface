package io.opencertflow.consulting;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.common.adapter.out.crypto.TextEncryptor;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 컨설팅 연결 엔드투엔드. 진단을 만든 뒤 그 결과에 상담을 붙이고, 연락처가 실제로 <b>암호화되어</b>
 * 저장되는지, 응답에는 <b>마스킹</b>되어 나가는지 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class ConsultingFlowIntegrationTest {

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
    JdbcTemplate jdbcTemplate;

    @Autowired
    TextEncryptor textEncryptor;

    private String createDiagnosis() {
        Map<String, Object> request = Map.of(
                "productName", "가정용 헤어드라이어", "productGroup", "SMALL_APPLIANCE",
                "usesElectricity", true, "ratedVoltage", 220, "powerConsumption", 1200,
                "hasBattery", false, "targetUser", "GENERAL", "salesChannel", "ONLINE",
                "materials", List.of("PLASTIC"), "heldDocuments", List.of("TEST_REPORT"));

        JsonNode created = webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(request).exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        return created.at("/data/id").asText();
    }

    @Test
    @DisplayName("상담 신청 → 201, 연락처는 마스킹 응답 + DB에는 암호화 저장")
    void 상담신청_암호화_저장() {
        String diagnosisId = createDiagnosis();
        Map<String, Object> lead = Map.of(
                "diagnosisId", diagnosisId,
                "contactName", "홍길동",
                "contactPhone", "010-1234-5678",
                "contactEmail", "hong@example.com",
                "message", "상담 원합니다",
                "privacyConsent", true,
                "sensitiveInfoConsent", true,
                "serviceLimitAcknowledged", true,
                "consentVersion", "v1");

        JsonNode response = webTestClient.post().uri("/api/v1/consulting-leads")
                .bodyValue(lead).exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        // 응답에는 마스킹된 연락처만
        assertThat(response.at("/data/maskedPhone").asText()).isEqualTo("****5678");
        assertThat(response.at("/data/maskedEmail").asText()).isEqualTo("ho***@example.com");
        assertThat(response.at("/data/status").asText()).isEqualTo("SUBMITTED");
        String leadId = response.at("/data/id").asText();

        // DB에는 평문이 없고, 복호화하면 원문이 나온다
        String storedPhone = jdbcTemplate.queryForObject(
                "SELECT contact_phone FROM consulting_lead WHERE id = ?", String.class,
                Long.parseLong(leadId));
        assertThat(storedPhone).isNotEqualTo("010-1234-5678");           // 평문 아님
        assertThat(textEncryptor.decrypt(storedPhone)).isEqualTo("010-1234-5678"); // 복호화 가능

        // 동의 로그가 함께 저장됐다
        Integer consentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consent_log WHERE consulting_lead_id = ?", Integer.class,
                Long.parseLong(leadId));
        assertThat(consentCount).isEqualTo(1);
    }

    @Test
    @DisplayName("개인정보 동의 없이 신청하면 400")
    void 동의없으면_400() {
        String diagnosisId = createDiagnosis();
        Map<String, Object> lead = Map.of(
                "diagnosisId", diagnosisId, "contactName", "홍길동", "contactPhone", "010-1234-5678",
                "privacyConsent", false, "sensitiveInfoConsent", false,
                "serviceLimitAcknowledged", false, "consentVersion", "v1");

        webTestClient.post().uri("/api/v1/consulting-leads")
                .bodyValue(lead).exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("존재하지 않는 진단에 상담을 붙이면 404")
    void 없는진단_404() {
        Map<String, Object> lead = Map.of(
                "diagnosisId", io.opencertflow.support.TestIds.nextString(),
                "contactName", "홍길동", "contactPhone", "010-1234-5678",
                "privacyConsent", true, "sensitiveInfoConsent", false,
                "serviceLimitAcknowledged", true, "consentVersion", "v1");

        webTestClient.post().uri("/api/v1/consulting-leads")
                .bodyValue(lead).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.error.code").isEqualTo("OCF-CONS-002");
    }
}
