package io.opencertflow.auth.adapter.out.redis;

import io.opencertflow.auth.application.port.out.RefreshTokenStorePort;
import io.opencertflow.auth.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 리프레시 토큰 저장소의 Redis 구현. Lettuce 기반이라 논블로킹이다.
 *
 * <p><b>세션(토큰)별 키.</b> 키는 {@code auth:refresh:<userId>:<tokenHash>}이고, 존재 자체가 그 세션이
 * 유효하다는 뜻이다(값은 자리표시자). 사용자당 하나로 덮어쓰지 않으므로 웹·앱·여러 기기의 동시
 * 로그인이 각각 독립된 세션으로 남는다(F-AUTH-013). TTL을 리프레시 토큰 수명과 같게 주어, 만료되면
 * Redis가 알아서 지운다.
 *
 * <p>토큰 원문 대신 SHA-256 해시를 키에 쓴다 — 키가 짧고 고정 길이가 되며, 저장소가 새더라도 원본
 * 토큰이 그대로 드러나지 않는다.
 */
@Component
public class RedisRefreshTokenStore implements RefreshTokenStorePort {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final String PRESENT = "1";

    private final ReactiveStringRedisTemplate redis;
    private final Duration ttl;

    public RedisRefreshTokenStore(ReactiveStringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(properties.jwt().refreshTokenTtlSeconds());
    }

    @Override
    public Mono<Void> save(String userId, String refreshToken) {
        return redis.opsForValue().set(sessionKey(userId, refreshToken), PRESENT, ttl).then();
    }

    @Override
    public Mono<Boolean> matches(String userId, String refreshToken) {
        return redis.hasKey(sessionKey(userId, refreshToken));
    }

    @Override
    public Mono<Void> delete(String userId, String refreshToken) {
        return redis.delete(sessionKey(userId, refreshToken)).then();
    }

    @Override
    public Mono<Void> deleteAll(String userId) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(KEY_PREFIX + userId + ":*").count(100).build();
        return redis.scan(options).flatMap(redis::delete).then();
    }

    private String sessionKey(String userId, String refreshToken) {
        return KEY_PREFIX + userId + ":" + hash(refreshToken);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM에 있다. 없으면 환경이 깨진 것이므로 그대로 터뜨린다.
            throw new IllegalStateException("SHA-256 미지원 환경", e);
        }
    }
}
