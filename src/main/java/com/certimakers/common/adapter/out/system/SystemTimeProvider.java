package com.certimakers.common.adapter.out.system;

import com.certimakers.common.domain.port.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * {@link TimeProvider}의 기본 구현. {@link Clock}에 위임하므로 테스트에서
 * {@code Clock.fixed(...)}로 시각을 고정할 수 있다.
 */
@Component
public class SystemTimeProvider implements TimeProvider {

    private final Clock clock;

    public SystemTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Instant now() {
        return clock.instant();
    }

    @Override
    public ZoneId zone() {
        return clock.getZone();
    }
}
