package com.certimakers.auth.adapter.out.redis;

import com.certimakers.auth.application.port.out.RefreshTokenStorePort;
import com.certimakers.auth.config.AuthProperties;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 리프레시 토큰 저장소의 Redis 구현. Lettuce 기반이라 논블로킹이다.
 *
 * <p>키는 {@code auth:refresh:<userId>}이고 값은 리프레시 토큰이다. TTL을 리프레시 토큰 수명과
 * 동일하게 주어, 만료되면 Redis가 알아서 지운다.
 */
@Component
public class RedisRefreshTokenStore implements RefreshTokenStorePort {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final ReactiveStringRedisTemplate redis;
    private final Duration ttl;

    public RedisRefreshTokenStore(ReactiveStringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(properties.jwt().refreshTokenTtlSeconds());
    }

    @Override
    public Mono<Void> save(String userId, String refreshToken) {
        return redis.opsForValue().set(key(userId), refreshToken, ttl).then();
    }

    @Override
    public Mono<Boolean> matches(String userId, String refreshToken) {
        return redis.opsForValue().get(key(userId))
                .map(stored -> stored.equals(refreshToken))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> delete(String userId) {
        return redis.opsForValue().delete(key(userId)).then();
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }
}
