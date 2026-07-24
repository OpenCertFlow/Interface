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
import java.util.HashMap;
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
 * 관리자 질문(입력 항목) 관리 API(F-WADM-006~008) 통합 검증.
 *
 * <p>입력 항목을 enum 코드로만 바꿀 수 있던 것을 프레젠테이션 오버라이드 API로 편집할 수 있는지를,
 * <b>편집 → 공개 스키마 반영</b>으로 확인한다. 코드 계약(코드·타입·의존)은 편집 대상이 아님을
 * 없는 코드 편집 거부로 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AdminProductGroupIntegrationTest {

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
                UserId.of(UUID.randomUUID()), Email.of("admin@certimakers.local"), null,
                Nickname.of("관리자"), Role.ADMIN, AuthProvider.LOCAL, null, true, Instant.now());
        return tokenProvider.issue(admin).accessToken();
    }

    private JsonNode publicSchema() {
        return webTestClient.get().uri("/api/v1/product-groups")
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();
    }

    private String labelOf(JsonNode schema, String group, String code) {
        for (JsonNode g : schema.at("/data")) {
            if (g.get("code").asText().equals(group)) {
                for (JsonNode f : g.get("fields")) {
                    if (f.get("code").asText().equals(code)) {
                        return f.get("label").asText();
                    }
                }
            }
        }
        return null;
    }

    @Test
    @DisplayName("관리자가 아니면 질문 관리 API에 접근할 수 없다")
    void 관리자가_아니면_접근할_수_없다() {
        webTestClient.get().uri("/api/v1/admin/product-groups/SMALL_APPLIANCE/questions")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("라벨을 편집하면 공개 스키마에 반영된다")
    void 라벨_편집이_공개_스키마에_반영된다() {
        assertThat(labelOf(publicSchema(), "SMALL_APPLIANCE", "productName")).isEqualTo("제품명");

        Map<String, Object> body = new HashMap<>();
        body.put("label", "제품 이름을 적어 주세요");
        body.put("active", true);

        webTestClient.put().uri("/api/v1/admin/product-groups/SMALL_APPLIANCE/questions/productName")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        assertThat(labelOf(publicSchema(), "SMALL_APPLIANCE", "productName"))
                .isEqualTo("제품 이름을 적어 주세요");
    }

    @Test
    @DisplayName("항목을 숨기면 공개 스키마에서 빠진다")
    void 숨긴_항목은_공개_스키마에서_빠진다() {
        // hasBattery는 기본 노출 항목이다.
        assertThat(labelOf(publicSchema(), "SMALL_APPLIANCE", "hasBattery")).isNotNull();

        Map<String, Object> body = new HashMap<>();
        body.put("active", false);

        webTestClient.put().uri("/api/v1/admin/product-groups/SMALL_APPLIANCE/questions/hasBattery")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();

        assertThat(labelOf(publicSchema(), "SMALL_APPLIANCE", "hasBattery")).isNull();
    }

    @Test
    @DisplayName("계약에 없는 코드는 편집할 수 없다 — 룰이 읽을 수 없는 질문을 만들 수 없다")
    void 없는_코드_편집은_거부된다() {
        Map<String, Object> body = new HashMap<>();
        body.put("label", "가짜 질문");
        body.put("active", true);

        webTestClient.put().uri("/api/v1/admin/product-groups/SMALL_APPLIANCE/questions/fake_code")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("편집용 목록은 숨김 항목까지 포함해 보여 준다")
    void 편집용_목록은_숨김까지_보여준다() {
        JsonNode list = webTestClient.get()
                .uri("/api/v1/admin/product-groups/ELECTRIC_HEATING_PAD/questions")
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        assertThat(list.at("/data").size())
                .isEqualTo(com.certimakers.diagnosis.domain.model.ProductGroup
                        .ELECTRIC_HEATING_PAD.inputFields().size());
    }
}
