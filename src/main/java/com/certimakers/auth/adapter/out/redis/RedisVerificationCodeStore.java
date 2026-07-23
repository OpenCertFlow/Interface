package com.certimakers.auth.adapter.out.redis;

import com.certimakers.auth.application.port.out.VerificationCodeStorePort;
import com.certimakers.auth.config.AuthProperties;
import com.certimakers.auth.domain.model.Email;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 이메일 인증 코드 저장소의 Redis 구현. TTL이 곧 "코드 유효 시간"이라는 도메인 규칙을 그대로 담는다.
 *
 * <p>키는 {@code auth:email-verify:<email>}이다.
 */
@Component
public class RedisVerificationCodeStore implements VerificationCodeStorePort {

    private static final String KEY_PREFIX = "auth:email-verify:";

    private final ReactiveStringRedisTemplate redis;
    private final Duration ttl;

    public RedisVerificationCodeStore(ReactiveStringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(properties.email().codeTtlSeconds());
    }

    @Override
    public Mono<Void> save(Email email, String code) {
        return redis.opsForValue().set(key(email), code, ttl).then();
    }

    @Override
    public Mono<Boolean> matches(Email email, String code) {
        return redis.opsForValue().get(key(email))
                .map(stored -> stored.equals(code))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> delete(Email email) {
        return redis.opsForValue().delete(key(email)).then();
    }

    private String key(Email email) {
        return KEY_PREFIX + email.value();
    }
}
