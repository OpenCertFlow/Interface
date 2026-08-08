package io.opencertflow.auth.adapter.out.redis;

import io.opencertflow.auth.application.port.out.PasswordResetTokenStorePort;
import io.opencertflow.auth.config.AuthProperties;
import io.opencertflow.auth.domain.model.Email;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 비밀번호 재설정 토큰 저장소의 Redis 구현. 토큰 → 이메일 방향으로 저장한다.
 *
 * <p>키는 {@code auth:pw-reset:<token>}이고 값은 대상 이메일이다. 링크에는 토큰만 담기고 이메일은
 * 서버가 되찾으므로, 이메일이 URL로 노출되지 않는다.
 */
@Component
public class RedisPasswordResetTokenStore implements PasswordResetTokenStorePort {

    private static final String KEY_PREFIX = "auth:pw-reset:";

    private final ReactiveStringRedisTemplate redis;
    private final Duration ttl;

    public RedisPasswordResetTokenStore(ReactiveStringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(properties.email().resetTokenTtlSeconds());
    }

    @Override
    public Mono<Void> save(String token, Email email) {
        return redis.opsForValue().set(key(token), email.value(), ttl).then();
    }

    @Override
    public Mono<Optional<Email>> findEmail(String token) {
        return redis.opsForValue().get(key(token))
                .map(value -> Optional.of(Email.of(value)))
                .defaultIfEmpty(Optional.empty());
    }

    @Override
    public Mono<Void> delete(String token) {
        return redis.opsForValue().delete(key(token)).then();
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }
}
