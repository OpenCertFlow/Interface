package io.opencertflow.auth.adapter.out.redis;

import io.opencertflow.auth.application.port.out.VerificationCodeStorePort;
import io.opencertflow.auth.config.AuthProperties;
import io.opencertflow.auth.domain.model.Email;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    /**
     * 저장된 코드와 대조한다.
     *
     * <p>{@link MessageDigest#isEqual}을 쓰는 이유는 비교 시간이 값에 따라 달라지지 않게 하기
     * 위함이다. {@code String.equals}는 첫 불일치 문자에서 즉시 끝나므로, 이론상 앞자리부터
     * 한 자리씩 맞춰 나갈 수 있다. 6자리 코드에 원격 타이밍 공격이 현실적이지는 않지만,
     * 바꾸는 비용이 0이고 "비밀값은 상수 시간으로 비교한다"는 규칙을 예외 없이 두는 편이 낫다.
     */
    @Override
    public Mono<Boolean> matches(Email email, String code) {
        if (code == null) {
            return Mono.just(false);
        }
        return redis.opsForValue().get(key(email))
                .map(stored -> MessageDigest.isEqual(
                        stored.getBytes(StandardCharsets.UTF_8),
                        code.getBytes(StandardCharsets.UTF_8)))
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
