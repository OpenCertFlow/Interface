package com.certimakers.auth;

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

/** 관리자 사용자 목록 조회(F-WADM-002) 통합 검증. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AdminUserListIntegrationTest {

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
                Nickname.of("관리자"), Role.ADMIN, AuthProvider.LOCAL, null, true,
                Instant.parse("2026-08-10T00:00:00Z"));
        return tokenProvider.issue(admin).accessToken();
    }

    private void signUp(String tag) {
        webTestClient.post().uri("/api/v1/auth/signup")
                .bodyValue(Map.of("email", tag + "@example.com", "password", "password1234", "nickname", tag))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    @DisplayName("관리자가 아니면 사용자 목록을 조회할 수 없다")
    void 관리자가_아니면_조회할_수_없다() {
        webTestClient.get().uri("/api/v1/admin/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("가입한 사용자가 목록에 나오고 역할로 필터링된다")
    void 사용자_목록과_역할_필터() {
        signUp("listuser1");
        signUp("listuser2");

        JsonNode all = webTestClient.get().uri("/api/v1/admin/users?role=USER")
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        assertThat(all.at("/data").size()).isGreaterThanOrEqualTo(2);
        for (JsonNode user : all.at("/data")) {
            assertThat(user.get("role").asText()).isEqualTo("USER");
            assertThat(user.has("email")).isTrue();
        }

        JsonNode admins = webTestClient.get().uri("/api/v1/admin/users?role=ADMIN")
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        // 가입한 사용자는 모두 USER라 ADMIN 필터에는 나오지 않는다(부트스트랩 관리자 없음).
        assertThat(admins.at("/data")).allMatch(u -> u.get("role").asText().equals("ADMIN"));
    }

    @Test
    @DisplayName("잘못된 역할 필터는 400을 낸다")
    void 잘못된_역할_필터는_거부된다() {
        webTestClient.get().uri("/api/v1/admin/users?role=WIZARD")
                .header("Authorization", "Bearer " + adminToken())
                .exchange()
                .expectStatus().isBadRequest();
    }
}
