package com.certimakers.consulting.adapter.in.scheduler;

import com.certimakers.consulting.application.port.in.PurgeExpiredLeadsUseCase;
import com.certimakers.consulting.config.PrivacyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 개인정보 보존 기간 파기를 주기적으로 트리거하는 인바운드 어댑터(F-BE-014).
 *
 * <p>스케줄 스레드에서 실행되므로 이벤트 루프를 막지 않는다. 실제 삭제는 유스케이스가
 * 블로킹 스케줄러 위에서 수행한다. 스케줄이 한 번 실패해도 다음 주기에 다시 시도한다.
 */
@Component
public class LeadRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeadRetentionScheduler.class);

    private final PurgeExpiredLeadsUseCase purgeExpiredLeadsUseCase;
    private final PrivacyProperties properties;

    public LeadRetentionScheduler(
            PurgeExpiredLeadsUseCase purgeExpiredLeadsUseCase, PrivacyProperties properties) {
        this.purgeExpiredLeadsUseCase = purgeExpiredLeadsUseCase;
        this.properties = properties;
    }

    // 애노테이션 플레이스홀더는 레코드의 @DefaultValue를 못 보므로 인라인 기본값을 함께 둔다.
    @Scheduled(cron = "${certimakers.privacy.purge-cron:0 30 3 * * *}")
    public void purgeExpiredLeads() {
        if (!properties.purgeEnabled()) {
            return;
        }
        purgeExpiredLeadsUseCase.purgeExpired().subscribe(
                deleted -> log.info(
                        "보존 기간 경과 리드 파기 완료 — {}건 삭제(보존 {}일)",
                        deleted, properties.leadRetentionDays()),
                error -> log.warn(
                        "보존 기간 리드 파기 실패 — 다음 주기에 재시도합니다. cause={}", error.toString()));
    }
}
