package com.certimakers.auth.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.auth.application.port.out.TokenProviderPort.AuthenticatedUser;
import com.certimakers.auth.application.port.out.TokenProviderPort.IssuedTokens;
import com.certimakers.auth.config.AuthProperties;
import com.certimakers.auth.domain.model.Email;
import com.certimakers.auth.domain.model.Nickname;
import com.certimakers.auth.domain.model.PasswordHash;
import com.certimakers.auth.domain.model.Role;
import com.certimakers.auth.domain.model.User;
import com.certimakers.auth.domain.model.UserId;
import com.certimakers.common.domain.port.TimeProvider;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JWT 발급·검증. 시각을 고정해 만료 동작까지 결정적으로 확인한다.
 *
 * <p>여기서 검증하는 것은 "우리가 서명한 유효한 토큰만 통과한다"이다. 이 규칙이 새면 인증 전체가
 * 무의미해진다.
 */
class JwtTokenProviderTest {

    private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");
    private static final String SECRET = "certimakers-test-secret-key-at-least-32-bytes-long";

    private record FixedTime(Instant now) implements TimeProvider {
        @Override
        public ZoneId zone() {
            return ZoneId.of("Asia/Seoul");
        }
    }

    private static AuthProperties properties(String secret) {
        return new AuthProperties(
                new AuthProperties.Jwt(secret, 1800, 1_209_600, "certimakers"),
                new AuthProperties.Kakao("id", "secret", "uri", "token", "userinfo"),
                new AuthProperties.EmailVerification(300, 1800, "http://localhost/reset", "no-reply@x.com"),
                java.util.List.of());
    }

    private static JwtTokenProvider providerAt(Instant now, String secret) {
        return new JwtTokenProvider(properties(secret), new FixedTime(now));
    }

    private static User user() {
        return User.registerLocal(
                UserId.of(UUID.fromString("01890000-0000-7000-8000-000000000001")),
                Email.of("user@example.com"),
                PasswordHash.of("$2a$10$hashed"),
                Nickname.of("테스터"),
                NOW);
    }

    @Test
    @DisplayName("발급한 액세스 토큰을 되읽으면 사용자 식별자와 권한이 복원된다")
    void 발급한_토큰을_되읽으면_주체가_복원된다() {
        JwtTokenProvider provider = providerAt(NOW, SECRET);
        User user = user();

        IssuedTokens tokens = provider.issue(user);
        Optional<AuthenticatedUser> parsed = provider.parseAccessToken(tokens.accessToken());

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(user.id().value().toString());
        assertThat(parsed.get().role()).isEqualTo(Role.USER.name());
        assertThat(tokens.accessTokenExpiresInSeconds()).isEqualTo(1800);
    }

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰은 서로 다른 값이다")
    void 액세스와_리프레시_토큰은_다르다() {
        IssuedTokens tokens = providerAt(NOW, SECRET).issue(user());

        assertThat(tokens.accessToken()).isNotEqualTo(tokens.refreshToken());
    }

    @Test
    @DisplayName("만료된 토큰은 통과시키지 않는다")
    void 만료된_토큰은_거부한다() {
        IssuedTokens tokens = providerAt(NOW, SECRET).issue(user());

        // 액세스 토큰 수명(1800초)을 넘긴 시점의 검증기
        JwtTokenProvider later = providerAt(NOW.plusSeconds(1801), SECRET);

        assertThat(later.parseAccessToken(tokens.accessToken())).isEmpty();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 통과시키지 않는다")
    void 다른_키로_서명된_토큰은_거부한다() {
        IssuedTokens forged = providerAt(NOW, "another-secret-key-at-least-32-bytes-long-x").issue(user());

        assertThat(providerAt(NOW, SECRET).parseAccessToken(forged.accessToken())).isEmpty();
    }

    @Test
    @DisplayName("형식이 깨진 문자열은 예외가 아니라 '인증 안 됨'으로 다룬다")
    void 형식이_깨진_토큰은_비어있음을_반환한다() {
        JwtTokenProvider provider = providerAt(NOW, SECRET);

        assertThat(provider.parseAccessToken("not-a-jwt")).isEmpty();
        assertThat(provider.parseAccessToken("")).isEmpty();
    }
}
