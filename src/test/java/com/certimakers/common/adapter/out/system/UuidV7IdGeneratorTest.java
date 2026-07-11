package com.certimakers.common.adapter.out.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.common.domain.port.TimeProvider;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UuidV7IdGeneratorTest {

    private static final Instant FIXED = Instant.parse("2026-07-10T04:00:00Z");

    /** 시각을 마음대로 조종할 수 있는 TimeProvider. 포트로 두었기에 가능한 일이다. */
    private static final class MutableTimeProvider implements TimeProvider {
        private Instant now;

        MutableTimeProvider(Instant now) {
            this.now = now;
        }

        @Override
        public Instant now() {
            return now;
        }

        @Override
        public ZoneId zone() {
            return ZoneOffset.UTC;
        }
    }

    @Test
    @DisplayName("RFC 9562의 버전(7)과 변형(RFC) 비트를 만족한다")
    void 버전과_변형_비트가_올바르다() {
        UuidV7IdGenerator generator = new UuidV7IdGenerator(new MutableTimeProvider(FIXED));

        UUID id = generator.nextId();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2); // 0b10
    }

    @Test
    @DisplayName("앞 48비트에 생성 시각의 Unix 밀리초가 그대로 들어간다")
    void 타임스탬프가_인코딩된다() {
        UuidV7IdGenerator generator = new UuidV7IdGenerator(new MutableTimeProvider(FIXED));

        UUID id = generator.nextId();

        long encodedMilli = id.getMostSignificantBits() >>> 16;
        assertThat(encodedMilli).isEqualTo(FIXED.toEpochMilli());
    }

    @Test
    @DisplayName("같은 밀리초 안에서 생성해도 생성 순서대로 정렬된다")
    void 같은_밀리초_내에서_단조_증가한다() {
        UuidV7IdGenerator generator = new UuidV7IdGenerator(new MutableTimeProvider(FIXED));

        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            ids.add(generator.nextId());
        }

        // 시각이 고정되어 있으므로 정렬은 오직 seq(rand_a 12비트)로 결정된다.
        for (int i = 1; i < ids.size(); i++) {
            long previous = ids.get(i - 1).getMostSignificantBits();
            long current = ids.get(i).getMostSignificantBits();
            assertThat(current)
                    .as("%d번째 ID가 이전 ID보다 커야 한다", i)
                    .isGreaterThan(previous);
        }
    }

    @Test
    @DisplayName("같은 밀리초에서 4096개를 넘기면 다음 밀리초를 당겨 써 단조성을 유지한다")
    void 시퀀스가_소진되면_다음_밀리초로_넘어간다() {
        UuidV7IdGenerator generator = new UuidV7IdGenerator(new MutableTimeProvider(FIXED));

        UUID first = generator.nextId();
        UUID last = null;
        for (int i = 0; i < 5_000; i++) {
            last = generator.nextId();
        }

        long firstMilli = first.getMostSignificantBits() >>> 16;
        long lastMilli = last.getMostSignificantBits() >>> 16;

        assertThat(firstMilli).isEqualTo(FIXED.toEpochMilli());
        assertThat(lastMilli).isGreaterThan(firstMilli);
        assertThat(last.getMostSignificantBits()).isGreaterThan(first.getMostSignificantBits());
    }

    @Test
    @DisplayName("시계가 뒤로 가도 과거로 되돌아가지 않는다")
    void 시계_역행에도_단조성을_유지한다() {
        MutableTimeProvider time = new MutableTimeProvider(FIXED);
        UuidV7IdGenerator generator = new UuidV7IdGenerator(time);

        UUID before = generator.nextId();
        time.now = FIXED.minusSeconds(10); // NTP 보정으로 시계가 뒤로 갔다
        UUID after = generator.nextId();

        assertThat(after.getMostSignificantBits()).isGreaterThan(before.getMostSignificantBits());
    }

    @Test
    @DisplayName("여러 스레드가 동시에 생성해도 중복이 없다")
    void 동시_생성_시_중복이_없다() throws Exception {
        UuidV7IdGenerator generator = new UuidV7IdGenerator(new MutableTimeProvider(FIXED));
        int threads = 8;
        int perThread = 500;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        Set<UUID> ids = java.util.Collections.synchronizedSet(new HashSet<>());

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                start.await();
                for (int i = 0; i < perThread; i++) {
                    ids.add(generator.nextId());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(ids).hasSize(threads * perThread);
    }
}
