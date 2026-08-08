package io.opencertflow.auth.adapter.out.redis;

import io.opencertflow.auth.application.port.out.AttemptLimiterPort;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 시도 횟수 제한의 Redis 구현. 고정 창(fixed window) 카운터다.
 *
 * <p>{@code INCR}로 세고, 값이 1이면(그 창의 첫 시도면) {@code EXPIRE}로 창을 연다. 창이 끝나면
 * 키가 사라져 카운터가 저절로 초기화된다 — 정리 배치가 필요 없다.
 *
 * <p>고정 창은 경계에서 최대 2배까지 허용한다(창 끝에 몰아 쓰고 다음 창 시작에 다시 몰아 쓰는 경우).
 * 슬라이딩 윈도로 막을 수 있지만, 여기서 막으려는 것은 100만 개 후보의 전수 탐색이다. 10회가
 * 20회가 되어도 그 공격은 여전히 불가능하므로 단순한 쪽을 택한다.
 *
 * <p>Redis가 죽으면 <b>통과시킨다.</b> 인증 저장소가 내려갔다고 로그인 자체를 막으면 장애가
 * 커진다. 무차별 대입 방어는 있으면 좋은 것이고, 로그인은 서비스의 본체다.
 */
@Component
public class RedisAttemptLimiter implements AttemptLimiterPort {

    private static final String KEY_PREFIX = "auth:attempts:";

    private final ReactiveStringRedisTemplate redis;

    public RedisAttemptLimiter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Boolean> exceeded(String key, Limit limit) {
        String redisKey = KEY_PREFIX + key;

        return redis.opsForValue().increment(redisKey)
                .flatMap(count -> count == 1L
                        // 창의 첫 시도에만 TTL을 건다. 매번 걸면 창이 계속 밀려 영원히 만료되지 않는다.
                        ? redis.expire(redisKey, Duration.ofSeconds(limit.windowSeconds()))
                                .thenReturn(count)
                        : Mono.just(count))
                .map(count -> count > limit.maxAttempts())
                .onErrorResume(error -> Mono.just(false));
    }

    @Override
    public Mono<Void> reset(String key) {
        return redis.delete(KEY_PREFIX + key)
                .onErrorResume(error -> Mono.just(0L))
                .then();
    }
}
