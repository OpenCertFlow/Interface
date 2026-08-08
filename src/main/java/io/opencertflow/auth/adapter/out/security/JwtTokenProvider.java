package io.opencertflow.auth.adapter.out.security;

import io.opencertflow.auth.application.port.out.TokenProviderPort;
import io.opencertflow.auth.config.AuthProperties;
import io.opencertflow.auth.domain.model.User;
import io.opencertflow.common.domain.port.TimeProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * {@link TokenProviderPort}의 JWT 구현.
 *
 * <p>서명·검증은 순수 CPU 연산이라 이벤트 루프에서 호출해도 블로킹이 아니다 — 시큐리티 필터가
 * 요청마다 {@link #parseAccessToken}을 직접 호출해도 안전하다.
 *
 * <p>액세스·리프레시 토큰 모두 같은 키로 서명한 JWT다. 차이는 만료 시간과, 리프레시 토큰이 Redis에
 * 저장되어 서버가 취소할 수 있다는 점뿐이다(순수 JWT는 취소가 불가능하므로).
 */
@Component
public class JwtTokenProvider implements TokenProviderPort {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;
    private final TimeProvider timeProvider;

    // 리프레시 토큰마다 유일한 jti를 붙이기 위한 카운터. 같은 밀리초에 두 번 로그인해도(웹·앱 동시)
    // 토큰이 서로 달라야 각각 독립된 세션으로 저장된다(F-AUTH-013). SecureRandom을 피해 이벤트 루프에서도 안전.
    private final AtomicLong tokenSequence = new AtomicLong();

    public JwtTokenProvider(AuthProperties properties, TimeProvider timeProvider) {
        AuthProperties.Jwt jwt = properties.jwt();
        this.signingKey = Keys.hmacShaKeyFor(jwt.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = jwt.accessTokenTtlSeconds();
        this.refreshTtlSeconds = jwt.refreshTokenTtlSeconds();
        this.issuer = jwt.issuer();
        this.timeProvider = timeProvider;
        warmUp();
    }

    /**
     * 서명·검증 경로를 시작 시점에 한 번 태워 둔다.
     *
     * <p>jjwt는 구현 클래스와 서비스 제공자를 <b>첫 사용 시점에 JAR에서 지연 로딩</b>한다. 그 로딩이
     * 파일 읽기라서, 첫 요청이 네티 이벤트 루프에서 파싱을 시도하면 실제 블로킹이 발생한다
     * (BlockHound가 {@code RandomAccessFile#readBytes}로 잡아낸다 — ADR-0002 위반).
     *
     * <p>시작 스레드에서 미리 로딩해 두면 이후 파싱은 순수 CPU 연산만 남아 이벤트 루프에서 안전하고,
     * 첫 요청의 지연 스파이크도 사라진다.
     */
    private void warmUp() {
        Instant now = timeProvider.now();
        String probe = buildToken("warm-up", "USER", now, 60, null);
        Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .clock(() -> Date.from(now))
                .build()
                .parseSignedClaims(probe);
    }

    @Override
    public IssuedTokens issue(User user) {
        Instant now = timeProvider.now();
        String subject = user.id().value().toString();
        String role = user.role().name();

        // 리프레시 토큰만 jti로 유일하게 만든다. 저장·회전 대상이 리프레시 토큰이기 때문이다.
        String accessToken = buildToken(subject, role, now, accessTtlSeconds, null);
        String refreshToken = buildToken(subject, role, now, refreshTtlSeconds, nextTokenId(now));
        return new IssuedTokens(accessToken, refreshToken, accessTtlSeconds);
    }

    @Override
    public Optional<AuthenticatedUser> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    // 만료 판정에도 TimeProvider를 쓴다. 기본값인 시스템 시계를 그대로 두면 발급은
                    // 주입된 시각으로, 검증은 실제 시각으로 이뤄져 두 기준이 어긋난다.
                    .clock(() -> Date.from(timeProvider.now()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(claims.getSubject(), claims.get(ROLE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치·만료·형식 오류는 예외가 아니라 '인증 안 됨'으로 다룬다. 필터가 익명 처리한다.
            return Optional.empty();
        }
    }

    /** 리프레시 토큰의 유일 식별자. {@code <발급밀리초>-<증가카운터>}라 같은 순간의 발급도 서로 다르다. */
    private String nextTokenId(Instant issuedAt) {
        return issuedAt.toEpochMilli() + "-" + tokenSequence.incrementAndGet();
    }

    private String buildToken(String subject, String role, Instant issuedAt, long ttlSeconds, String jti) {
        Instant expiry = issuedAt.plusSeconds(ttlSeconds);
        var builder = Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiry));
        if (jti != null) {
            builder.id(jti); // JWT 표준 jti 클레임
        }
        return builder.signWith(signingKey).compact();
    }
}
