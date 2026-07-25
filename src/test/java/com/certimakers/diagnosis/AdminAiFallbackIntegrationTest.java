package com.certimakers.diagnosis;

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

/** AI 장애 폴백 설정(F-WADM-020) 통합 검증. 폴백을 켜면 진단이 근거·문장 없이 즉시 응답한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AdminAiFallbackIntegrationTest {

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

    private String adminToken() {
        User admin = User.reconstitute(
                UserId.of(com.certimakers.support.TestIds.next()), Email.of("admin@certimakers.local"), null,
                Nickname.of("관리자"), Role.ADMIN, AuthProvider.LOCAL, null, true,
                Instant.parse("2026-08-10T00:00:00Z"));
        return tokenProvider.issue(admin).accessToken();
    }

    private JsonNode diagnoseHairDryer() {
        Map<String, Object> request = new java.util.HashMap<>();
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
                .exchange().expectStatus().isCreated()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    @Test
    @DisplayName("관리자가 아니면 폴백 설정에 접근할 수 없다")
    void 관리자가_아니면_접근할_수_없다() {
        webTestClient.get().uri("/api/v1/admin/ai-fallback")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("폴백을 켜면 진단이 근거·문장 없이(degraded) 응답한다")
    void 폴백을_켜면_degraded로_응답한다() {
        // 기본은 꺼짐.
        JsonNode initial = webTestClient.get().uri("/api/v1/admin/ai-fallback")
                .header("Authorization", "Bearer " + adminToken())
                .exchange().expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertThat(initial.at("/data/evidenceDisabled").asBoolean()).isFalse();

        // 폴백 켜기.
        webTestClient.put().uri("/api/v1/admin/ai-fallback")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(Map.of("evidenceDisabled", true, "narrationDisabled", true))
                .exchange().expectStatus().isOk();

        // 진단은 근거·문장이 degraded여야 한다.
        JsonNode report = diagnoseHairDryer();
        assertThat(report.at("/data/degraded/evidence").asBoolean()).isTrue();
        assertThat(report.at("/data/degraded/narration").asBoolean()).isTrue();

        // 원복.
        webTestClient.put().uri("/api/v1/admin/ai-fallback")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(Map.of("evidenceDisabled", false, "narrationDisabled", false))
                .exchange().expectStatus().isOk();
    }
}
