package com.certimakers.common.adapter.out.system;

import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * RFC 9562 UUIDv7 생성기. 앞 48비트가 Unix 밀리초라 시간순으로 정렬된다.
 *
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    unix_ts_ms (48 bits)                       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          unix_ts_ms           |  ver  |   seq (12 bits)       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |var|                   rand_b (62 bits)                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * <p>{@code rand_a} 12비트를 난수 대신 <b>같은 밀리초 내 시퀀스</b>로 쓴다(RFC 9562 §6.2 method 1).
 * 덕분에 밀리초당 4096개까지 생성 순서가 그대로 정렬 순서가 되고, 동시 삽입된 행들도
 * B-tree 인덱스에서 인접한다. 시퀀스가 소진되면 다음 밀리초를 미리 당겨 쓴다.
 *
 * <p>시계가 뒤로 가도 단조성이 유지된다 — 과거 밀리초로 되돌아가지 않고 마지막 값에서 이어간다.
 */
@Component
public class UuidV7IdGenerator implements IdGenerator {

    private static final int MAX_SEQUENCE = 0xFFF;
    private static final long TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL;
    private static final long VERSION_7 = 0x7000L;
    private static final long VARIANT_RFC9562 = 0x8000_0000_0000_0000L;
    private static final long RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;

    private final TimeProvider timeProvider;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AtomicReference<Tick> lastTick = new AtomicReference<>(new Tick(Long.MIN_VALUE, 0));

    public UuidV7IdGenerator(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public UUID nextId() {
        long observedMilli = timeProvider.now().toEpochMilli();
        Tick tick = lastTick.updateAndGet(previous -> advance(previous, observedMilli));

        long mostSignificant = ((tick.epochMilli() & TIMESTAMP_MASK) << 16) | VERSION_7 | tick.sequence();
        long leastSignificant = (secureRandom.nextLong() & RAND_B_MASK) | VARIANT_RFC9562;
        return new UUID(mostSignificant, leastSignificant);
    }

    /**
     * 순수 함수여야 한다. {@code updateAndGet}은 CAS 경합 시 이 람다를 여러 번 호출하므로,
     * 여기서 시각을 다시 읽거나 난수를 뽑으면 안 된다.
     */
    private static Tick advance(Tick previous, long observedMilli) {
        if (observedMilli > previous.epochMilli()) {
            return new Tick(observedMilli, 0);
        }
        if (previous.sequence() < MAX_SEQUENCE) {
            return new Tick(previous.epochMilli(), previous.sequence() + 1);
        }
        // 같은 밀리초에서 4096개를 모두 썼다. 다음 밀리초를 당겨 쓴다.
        return new Tick(previous.epochMilli() + 1, 0);
    }

    private record Tick(long epochMilli, int sequence) {
    }
}
