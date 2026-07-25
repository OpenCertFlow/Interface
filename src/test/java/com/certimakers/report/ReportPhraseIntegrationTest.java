package com.certimakers.report;

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

/** 리포트 문구 관리(F-WADM-016) 통합 검증. 공개 조회 + 관리자 편집. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class ReportPhraseIntegrationTest {

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

    private String textOf(JsonNode list, String key) {
        for (JsonNode p : list.at("/data")) {
            if (p.get("key").asText().equals(key)) {
                return p.get("text").asText();
            }
        }
        return null;
    }

    private JsonNode publicPhrases() {
        return webTestClient.get().uri("/api/v1/report-phrases")
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    @Test
    @DisplayName("공개 조회로 시드된 문구를 볼 수 있다")
    void 공개_조회로_시드_문구를_본다() {
        assertThat(textOf(publicPhrases(), "REPORT_DISCLAIMER")).contains("사전 점검");
    }

    @Test
    @DisplayName("관리자가 문구를 편집하면 공개 조회에 반영된다")
    void 관리자_편집이_공개_조회에_반영된다() {
        webTestClient.put().uri("/api/v1/admin/report-phrases/REPORT_DISCLAIMER")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(Map.of("text", "개정된 면책 문구입니다.", "description", "면책"))
                .exchange()
                .expectStatus().isOk();

        assertThat(textOf(publicPhrases(), "REPORT_DISCLAIMER")).isEqualTo("개정된 면책 문구입니다.");
    }

    @Test
    @DisplayName("관리자가 아니면 문구를 편집할 수 없다")
    void 관리자가_아니면_편집할_수_없다() {
        webTestClient.put().uri("/api/v1/admin/report-phrases/REPORT_DISCLAIMER")
                .bodyValue(Map.of("text", "무단 편집"))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
