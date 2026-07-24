package com.certimakers.consulting;

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
import java.util.List;
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
 * 컨설턴트 상담 워크플로(F-WCON) 통합 검증. 진단 → 상담 접수 → 컨설턴트 조회·배정·상태 전이·메모 →
 * 완료의 전 과정과 권한·전이 규칙을 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class ConsultingWorkflowIntegrationTest {

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

    private final String consultantId = UUID.randomUUID().toString();

    private String tokenFor(Role role, String id) {
        User user = User.reconstitute(
                UserId.of(UUID.fromString(id)), Email.of(role + "@certimakers.local"), null,
                Nickname.of(role.name()), role, AuthProvider.LOCAL, null, true,
                Instant.parse("2026-08-10T00:00:00Z"));
        return tokenProvider.issue(user).accessToken();
    }

    private String consultantToken() {
        return tokenFor(Role.CONSULTANT, consultantId);
    }

    private String createDiagnosis() {
        Map<String, Object> request = new HashMap<>();
        request.put("productName", "가정용 헤어드라이어");
        request.put("productGroup", "SMALL_APPLIANCE");
        request.put("usesElectricity", true);
        request.put("ratedVoltage", 220);
        request.put("powerConsumption", 1200);
        request.put("hasBattery", false);
        request.put("targetUser", "GENERAL");
        request.put("salesChannel", "ONLINE");
        request.put("materials", List.of("PLASTIC"));
        request.put("heldDocuments", List.of());
        return webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody()
                .at("/data/id").asText();
    }

    private void submitLead(String diagnosisId) {
        Map<String, Object> request = new HashMap<>();
        request.put("diagnosisId", diagnosisId);
        request.put("contactName", "김소공");
        request.put("contactPhone", "010-1234-5678");
        request.put("message", "인증 상담 원합니다");
        request.put("privacyConsent", true);
        request.put("sensitiveInfoConsent", false);
        request.put("serviceLimitAcknowledged", true);
        request.put("consentVersion", "v1");
        webTestClient.post().uri("/api/v1/consulting-leads")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
    }

    private JsonNode leadDetail(String leadId) {
        return webTestClient.get().uri("/api/v1/consulting/leads/{id}", leadId)
                .header("Authorization", "Bearer " + consultantToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    private String firstLeadId() {
        JsonNode list = webTestClient.get().uri("/api/v1/consulting/leads?status=SUBMITTED")
                .header("Authorization", "Bearer " + consultantToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        return list.at("/data").get(0).get("id").asText();
    }

    @Test
    @DisplayName("컨설턴트가 아니면 상담 처리 API에 접근할 수 없다")
    void 권한이_없으면_접근할_수_없다() {
        webTestClient.get().uri("/api/v1/consulting/leads")
                .exchange()
                .expectStatus().isUnauthorized();

        // 일반 사용자(USER)는 접근 금지(403).
        webTestClient.get().uri("/api/v1/consulting/leads")
                .header("Authorization", "Bearer " + tokenFor(Role.USER, UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("접수 → 배정 → 진행 → 메모 → 완료의 전 과정이 동작한다")
    void 상담_워크플로_전과정() {
        submitLead(createDiagnosis());
        String leadId = firstLeadId();

        // 접수 상태이며 연락처가 복호화돼 보인다.
        JsonNode submitted = leadDetail(leadId);
        assertThat(submitted.at("/data/status").asText()).isEqualTo("SUBMITTED");
        assertThat(submitted.at("/data/contactPhone").asText()).isEqualTo("010-1234-5678");

        // 배정 → 담당이 현재 컨설턴트로 지정되고 ASSIGNED로 넘어간다.
        JsonNode assigned = webTestClient.post().uri("/api/v1/consulting/leads/{id}/assign", leadId)
                .header("Authorization", "Bearer " + consultantToken())
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(assigned.at("/data/status").asText()).isEqualTo("ASSIGNED");
        assertThat(assigned.at("/data/assignedConsultantId").asText()).isEqualTo(consultantId);

        // 진행 → 메모 → 완료.
        transition(leadId, "IN_PROGRESS");
        webTestClient.put().uri("/api/v1/consulting/leads/{id}/memo", leadId)
                .header("Authorization", "Bearer " + consultantToken())
                .bodyValue(Map.of("memo", "1차 상담 완료, 시험성적서 안내"))
                .exchange().expectStatus().isOk();
        transition(leadId, "COMPLETED");

        JsonNode done = leadDetail(leadId);
        assertThat(done.at("/data/status").asText()).isEqualTo("COMPLETED");
        assertThat(done.at("/data/internalMemo").asText()).contains("시험성적서");
    }

    @Test
    @DisplayName("허용되지 않은 상태 전이는 거부된다 — 접수에서 바로 완료로 갈 수 없다")
    void 잘못된_전이는_거부된다() {
        submitLead(createDiagnosis());
        String leadId = firstLeadId();

        webTestClient.post().uri("/api/v1/consulting/leads/{id}/status", leadId)
                .header("Authorization", "Bearer " + consultantToken())
                .bodyValue(Map.of("status", "COMPLETED"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    private void transition(String leadId, String status) {
        webTestClient.post().uri("/api/v1/consulting/leads/{id}/status", leadId)
                .header("Authorization", "Bearer " + consultantToken())
                .bodyValue(Map.of("status", status))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("컨설턴트 메시지: 공개 메시지는 소공인이 리드 id로 보고, 내부 메모는 숨겨진다")
    void 상담_메시지_스레드() {
        submitLead(createDiagnosis());
        String leadId = firstLeadId();

        postMessage(leadId, "INFO_REQUEST", "회로도를 보내주세요");
        postMessage(leadId, "NOTE", "내부 메모: 등급 확인 필요");

        // 컨설턴트는 전체(공개 1 + 내부 1)를 본다.
        JsonNode all = webTestClient.get().uri("/api/v1/consulting/leads/{id}/messages", leadId)
                .header("Authorization", "Bearer " + consultantToken())
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(all.at("/data").size()).isEqualTo(2);

        // 소공인은 리드 id로 공개 메시지만 본다(내부 메모 제외).
        JsonNode publicMessages = webTestClient.get()
                .uri("/api/v1/consulting-leads/{id}/messages", leadId)
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(publicMessages.at("/data").size()).isEqualTo(1);
        assertThat(publicMessages.at("/data").get(0).get("kind").asText()).isEqualTo("INFO_REQUEST");
        assertThat(publicMessages.at("/data").get(0).get("body").asText()).contains("회로도");
    }

    private void postMessage(String leadId, String kind, String body) {
        webTestClient.post().uri("/api/v1/consulting/leads/{id}/messages", leadId)
                .header("Authorization", "Bearer " + consultantToken())
                .bodyValue(Map.of("kind", kind, "body", body))
                .exchange()
                .expectStatus().isCreated();
    }
}
