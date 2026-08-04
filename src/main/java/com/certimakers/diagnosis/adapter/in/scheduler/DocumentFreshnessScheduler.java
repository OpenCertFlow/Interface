package com.certimakers.diagnosis.adapter.in.scheduler;

import com.certimakers.diagnosis.application.port.in.MonitorDocumentFreshnessUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 공식 문서 원문 변경을 주기적으로 살피는 인바운드 어댑터.
 *
 * <p>기본값은 <b>꺼짐</b>이다. 외부 사이트를 주기적으로 두드리는 동작이라 시연·개발 환경에서
 * 저절로 도는 것은 바람직하지 않고, 운영에서 켜는 것이 명시적 선택이어야 한다.
 *
 * <p>실패해도 다음 주기에 다시 시도한다. 개별 문서의 접근 실패는 유스케이스가 이미 삼킨다.
 */
@Component
public class DocumentFreshnessScheduler {

    private static final Logger log = LoggerFactory.getLogger(DocumentFreshnessScheduler.class);

    private final MonitorDocumentFreshnessUseCase monitorUseCase;
    private final boolean enabled;

    public DocumentFreshnessScheduler(
            MonitorDocumentFreshnessUseCase monitorUseCase,
            @Value("${certimakers.document-freshness.enabled:false}") boolean enabled) {
        this.monitorUseCase = monitorUseCase;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${certimakers.document-freshness.cron:0 0 4 * * MON}")
    public void checkFreshness() {
        if (!enabled) {
            return;
        }
        monitorUseCase.checkAll().subscribe(
                summary -> log.info(
                        "공식 문서 신선도 점검 — 확인 {}건, 변경 {}건, 접근 실패 {}건",
                        summary.checked(), summary.changed(), summary.unreachable()),
                error -> log.warn(
                        "공식 문서 신선도 점검 실패 — 다음 주기에 재시도합니다. cause={}",
                        error.toString()));
    }
}
