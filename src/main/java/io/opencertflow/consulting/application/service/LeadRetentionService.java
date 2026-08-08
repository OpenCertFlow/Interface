package io.opencertflow.consulting.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.consulting.application.port.in.PurgeExpiredLeadsUseCase;
import io.opencertflow.consulting.application.port.out.PurgeLeadsPort;
import io.opencertflow.consulting.config.PrivacyProperties;
import java.time.Duration;
import java.time.Instant;
import reactor.core.publisher.Mono;

/**
 * 보존 기간 경과 리드 파기(F-BE-014). 기준 시각은 {@code now - leadRetentionDays}이며, 그보다 오래된
 * 종착 리드를 삭제한다. 삭제는 블로킹 JDBC라 {@link BlockingBridge}로 스케줄러 밖에서 돌린다.
 */
@UseCase
public class LeadRetentionService implements PurgeExpiredLeadsUseCase {

    private final PurgeLeadsPort purgeLeadsPort;
    private final BlockingBridge blockingBridge;
    private final TimeProvider timeProvider;
    private final PrivacyProperties properties;

    public LeadRetentionService(
            PurgeLeadsPort purgeLeadsPort,
            BlockingBridge blockingBridge,
            TimeProvider timeProvider,
            PrivacyProperties properties) {
        this.purgeLeadsPort = purgeLeadsPort;
        this.blockingBridge = blockingBridge;
        this.timeProvider = timeProvider;
        this.properties = properties;
    }

    @Override
    public Mono<Long> purgeExpired() {
        Instant threshold = timeProvider.now().minus(Duration.ofDays(properties.leadRetentionDays()));
        return blockingBridge.mono(() -> purgeLeadsPort.deleteTerminalOlderThan(threshold));
    }
}
