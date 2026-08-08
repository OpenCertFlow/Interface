package io.opencertflow.common.adapter.in.web.trace;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.common.domain.port.TimeProvider;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 추적 ID 생성기. 이벤트 루프에서 매 요청 호출되므로 블로킹하지 않아야 하고, 로그 상관 키로 쓰이므로
 * 충돌하지 않아야 한다.
 */
class TraceIdFactoryTest {

    private static final Instant FIXED = Instant.parse("2026-08-10T12:00:00Z");

    /** 시각을 고정한 TimeProvider. 포트로 두었기에 가능한 일이다. */
    private record FixedTime(Instant now) implements TimeProvider {
        @Override
        public ZoneId zone() {
            return ZoneId.of("Asia/Seoul");
        }
    }

    private final TraceIdFactory factory = new TraceIdFactory(new FixedTime(FIXED));

    @Test
    @DisplayName("UUIDv7 형식이며 앞부분이 요청 시각이라 로그가 시간순으로 정렬된다")
    void UUIDv7_형식이고_시간순으로_정렬된다() {
        UUID id = UUID.fromString(factory.newTraceId());

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);

        long epochMilliInId = id.getMostSignificantBits() >>> 16;
        assertThat(epochMilliInId).isEqualTo(FIXED.toEpochMilli());
    }

    @Test
    @DisplayName("같은 밀리초에 대량 생성해도 충돌하지 않는다")
    void 같은_밀리초에_대량_생성해도_충돌하지_않는다() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            generated.add(factory.newTraceId());
        }

        assertThat(generated).hasSize(10_000);
    }

    @Test
    @DisplayName("나중 시각에 만든 ID가 문자열 정렬에서도 뒤에 온다")
    void 나중_시각의_ID가_문자열_정렬에서도_뒤에_온다() {
        String earlier = new TraceIdFactory(new FixedTime(FIXED)).newTraceId();
        String later = new TraceIdFactory(new FixedTime(FIXED.plusSeconds(1))).newTraceId();

        assertThat(earlier).isLessThan(later);
    }
}
