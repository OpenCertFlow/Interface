package io.opencertflow.common.adapter.in.web.trace;

import io.opencertflow.common.domain.port.TimeProvider;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * 요청 추적 ID 생성기. <b>네티 이벤트 루프에서 호출되므로 절대 블로킹하면 안 된다.</b>
 *
 * <p>도메인 식별자와 달리 {@code SecureRandom}을 쓰지 않는다. 리눅스에서 {@code SecureRandom}은
 * {@code NativePRNG}를 통해 {@code /dev/urandom}을 <b>파일로 읽는다</b>. 그 읽기가 이벤트 루프에서
 * 일어나면 ADR-0002가 금지하는 블로킹 호출이 되고, BlockHound가 실제로 잡아낸다. 윈도우는 다른 PRNG
 * 공급자를 쓰기 때문에 로컬 개발에서는 드러나지 않는다 — 배포 대상이 리눅스이므로 반드시 피해야 한다.
 *
 * <p>추적 ID에 필요한 성질은 <b>충돌하지 않음</b>이지 <b>추측 불가</b>가 아니다. 응답 헤더로 그대로
 * 돌려주는 로그 상관 키일 뿐이므로 {@link ThreadLocalRandom}으로 충분하다.
 *
 * <p>반면 진단·리드 식별자는 인증 없이 리포트에 접근하는 사실상의 토큰이므로 추측 불가해야 한다.
 * 그쪽은 계속 {@code SecureRandom} 기반 {@code IdGenerator}를 쓰며, 그 생성은 블로킹 스케줄러에서
 * 일어나므로 문제가 되지 않는다.
 *
 * <p>앞 48비트에 Unix 밀리초를 두는 UUIDv7 배치를 유지한다. 로그를 추적 ID로 정렬하면 시간순이 된다.
 */
@Component
public class TraceIdFactory {

    private static final long TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL;
    private static final long VERSION_7 = 0x7000L;
    private static final long VARIANT_RFC9562 = 0x8000_0000_0000_0000L;
    private static final long RAND_A_MASK = 0xFFFL;
    private static final long RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;

    private final TimeProvider timeProvider;

    public TraceIdFactory(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public String newTraceId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long epochMilli = timeProvider.now().toEpochMilli();

        long mostSignificant =
                ((epochMilli & TIMESTAMP_MASK) << 16) | VERSION_7 | (random.nextLong() & RAND_A_MASK);
        long leastSignificant = (random.nextLong() & RAND_B_MASK) | VARIANT_RFC9562;

        return new UUID(mostSignificant, leastSignificant).toString();
    }
}
