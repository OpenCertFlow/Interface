package com.certimakers.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 인증 엔드투엔드 검증. 실제 PostgreSQL·Redis 위에서 회원가입 → 로그인 → 마이페이지까지 한 흐름으로
 * 돈다.
 *
 * <p>이메일 발송({@code MailSender})만 목으로 대체한다. SMTP 서버를 띄우는 것은 이 테스트의 관심사가
 * 아니며, 검증 대상은 "코드가 Redis에 저장되고 대조된다"이지 "메일이 실제로 도착한다"가 아니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class AuthFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    WebTestClient webTestClient;

    /**
     * 실제 SMTP 없이 돌린다. 발송 호출 자체는 일어나되 네트워크로 나가지 않는다.
     *
     * <p>{@code MailSender}가 아니라 {@code JavaMailSender}를 대체하는 이유는, 자동 구성이 만드는
     * 빈의 타입이 {@code JavaMailSender}이기 때문이다. 상위 타입으로 대체하면 원래 빈이 사라져
     * 액추에이터의 메일 헬스 기여자가 초기화에 실패한다.
     */
    @MockitoBean
    JavaMailSender mailSender;

    private static final String PASSWORD = "password1234";

    /**
     * 테스트마다 다른 이메일을 쓴다. 모든 테스트가 같은 컨텍스트·같은 DB를 공유하므로 상수 하나를
     * 돌려쓰면 두 번째 테스트부터 중복 가입으로 실패한다.
     */
    private static String uniqueEmail(String tag) {
        return tag + "@example.com";
    }

    private String signUp(String email, String nickname) {
        webTestClient.post().uri("/api/v1/auth/signup")
                .bodyValue(Map.of("email", email, "password", PASSWORD, "nickname", nickname))
                .exchange()
                .expectStatus().isCreated();
        return email;
    }

    private String loginAndGetAccessToken(String email) {
        JsonNode login = webTestClient.post().uri("/api/v1/auth/login")
                .bodyValue(Map.of("email", email, "password", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class)
                .returnResult().getResponseBody();

        return login.at("/data/accessToken").asText();
    }

    private String signUpAndLogin(String tag, String nickname) {
        return loginAndGetAccessToken(signUp(uniqueEmail(tag), nickname));
    }

    @Test
    @DisplayName("계정 탈퇴 후에는 다시 로그인할 수 없다 (F-AUTH-018)")
    void 계정_탈퇴() {
        String email = uniqueEmail("withdraw-flow");
        String accessToken = loginAndGetAccessToken(signUp(email, "탈퇴예정"));

        webTestClient.delete().uri("/api/v1/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isNoContent();

        // 계정이 사라졌으므로 재로그인은 실패한다.
        webTestClient.post().uri("/api/v1/auth/login")
                .bodyValue(Map.of("email", email, "password", PASSWORD))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("회원가입 → 로그인 → 마이페이지 조회까지 한 흐름으로 동작한다")
    void 회원가입_로그인_마이페이지_전체흐름() {
        String email = uniqueEmail("flow");
        String accessToken = loginAndGetAccessToken(signUp(email, "테스터"));
        assertThat(accessToken).isNotBlank();

        webTestClient.get().uri("/api/v1/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.email").isEqualTo(email)
                .jsonPath("$.data.nickname").isEqualTo("테스터")
                .jsonPath("$.data.provider").isEqualTo("LOCAL")
                .jsonPath("$.data.emailVerified").isEqualTo(false);
    }

    @Test
    @DisplayName("마이페이지는 토큰 없이 접근할 수 없다")
    void 토큰_없이_마이페이지에_접근할_수_없다() {
        webTestClient.get().uri("/api/v1/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("CM-AUTH-007");
    }

    @Test
    @DisplayName("위조된 토큰으로는 접근할 수 없다")
    void 위조된_토큰으로_접근할_수_없다() {
        webTestClient.get().uri("/api/v1/me")
                .header("Authorization", "Bearer forged.token.value")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("같은 이메일로 두 번 가입할 수 없다")
    void 같은_이메일로_두_번_가입할_수_없다() {
        webTestClient.post().uri("/api/v1/auth/signup")
                .bodyValue(Map.of("email", "dup@example.com", "password", PASSWORD, "nickname", "중복"))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/api/v1/auth/signup")
                .bodyValue(Map.of("email", "dup@example.com", "password", PASSWORD, "nickname", "중복2"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("CM-AUTH-001");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 이메일 존재 여부를 알려주지 않고 같은 오류로 답한다")
    void 잘못된_자격증명은_동일한_오류를_준다() {
        webTestClient.post().uri("/api/v1/auth/signup")
                .bodyValue(Map.of("email", "creds@example.com", "password", PASSWORD, "nickname", "자격"))
                .exchange()
                .expectStatus().isCreated();

        // 존재하는 계정 + 틀린 비밀번호
        webTestClient.post().uri("/api/v1/auth/login")
                .bodyValue(Map.of("email", "creds@example.com", "password", "wrong-password"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-AUTH-002");

        // 존재하지 않는 계정 — 같은 코드여야 가입 여부가 새지 않는다
        webTestClient.post().uri("/api/v1/auth/login")
                .bodyValue(Map.of("email", "nobody@example.com", "password", PASSWORD))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-AUTH-002");
    }

    @Test
    @DisplayName("닉네임을 수정하면 마이페이지에 반영된다")
    void 닉네임을_수정할_수_있다() {
        String accessToken = signUpAndLogin("nickname-change", "원래이름");

        webTestClient.patch().uri("/api/v1/me/nickname")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(Map.of("nickname", "바뀐이름"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.nickname").isEqualTo("바뀐이름");
    }

    @Test
    @DisplayName("리프레시 토큰으로 새 액세스 토큰을 받는다")
    void 리프레시_토큰으로_재발급받는다() {
        webTestClient.post().uri("/api/v1/auth/signup")
                .bodyValue(Map.of("email", "refresh@example.com", "password", PASSWORD, "nickname", "리프레시"))
                .exchange()
                .expectStatus().isCreated();

        JsonNode login = webTestClient.post().uri("/api/v1/auth/login")
                .bodyValue(Map.of("email", "refresh@example.com", "password", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class).returnResult().getResponseBody();

        String refreshToken = login.at("/data/refreshToken").asText();

        webTestClient.post().uri("/api/v1/auth/refresh")
                .bodyValue(Map.of("refreshToken", refreshToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.accessToken").isNotEmpty();
    }

    @Test
    @DisplayName("이메일 인증 코드를 받아 확인하면 인증 완료 상태가 된다")
    void 이메일_인증_코드로_인증을_완료한다() {
        String email = uniqueEmail("email-verify");
        String accessToken = loginAndGetAccessToken(signUp(email, "인증"));

        webTestClient.post().uri("/api/v1/auth/email/code")
                .bodyValue(Map.of("email", email))
                .exchange()
                .expectStatus().isAccepted();

        // 잘못된 코드는 거부된다. 올바른 코드는 메일로만 전달되므로 여기서는 거부 경로를 검증한다.
        webTestClient.post().uri("/api/v1/auth/email/verify")
                .bodyValue(Map.of("email", email, "code", "000000"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error.code").isEqualTo("CM-AUTH-004");

        // 인증 실패했으므로 여전히 미인증이다
        webTestClient.get().uri("/api/v1/me")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.emailVerified").isEqualTo(false);
    }

    @Test
    @DisplayName("비밀번호 재설정 요청은 가입되지 않은 이메일이어도 성공으로 응답한다 — 계정 열거 방지")
    void 재설정_요청은_계정_존재를_노출하지_않는다() {
        webTestClient.post().uri("/api/v1/auth/password/reset-request")
                .bodyValue(Map.of("email", "never-registered@example.com"))
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    @DisplayName("기존 진단 API는 인증 없이 그대로 쓸 수 있다 — 시연 흐름을 막지 않는다")
    void 기존_진단_API는_인증_없이_동작한다() {
        webTestClient.post().uri("/api/v1/diagnoses")
                .bodyValue(Map.of(
                        "productName", "가정용 헤어드라이어",
                        "productGroup", "SMALL_APPLIANCE",
                        "usesElectricity", true,
                        "ratedVoltage", 220,
                        "powerConsumption", 1200,
                        "hasBattery", false,
                        "targetUser", "GENERAL",
                        "salesChannel", "ONLINE",
                        "materials", java.util.List.of("PLASTIC"),
                        "heldDocuments", java.util.List.of()))
                .exchange()
                .expectStatus().isCreated();
    }
}
