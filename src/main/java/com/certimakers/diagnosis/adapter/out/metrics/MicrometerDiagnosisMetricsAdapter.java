package com.certimakers.diagnosis.adapter.out.metrics;

import com.certimakers.common.adapter.out.external.annotation.ExternalAdapter;
import com.certimakers.diagnosis.application.port.out.DiagnosisMetricsPort;
import com.certimakers.diagnosis.domain.model.ChecklistItem;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;

/**
 * 진단 도메인 지표를 Micrometer로 내보낸다. {@code /actuator/metrics}에서 조회한다.
 *
 * <p>재는 것은 발표에서 주장할 수 있는 값들이다. 특히 <b>저하 비율</b>이 중요하다 —
 * 근거·설명이 없는 리포트가 얼마나 나가는지는 서비스 신뢰도를 직접 나타내고, 색인 공백이 생기면
 * 여기서 가장 먼저 드러난다.
 *
 * <p>모든 메서드가 예외를 삼킨다. 지표 때문에 진단이 실패하면 본말이 전도된다.
 */
@ExternalAdapter
public class MicrometerDiagnosisMetricsAdapter implements DiagnosisMetricsPort {

    private static final String PREFIX = "certimakers.diagnosis";

    private final MeterRegistry registry;

    public MicrometerDiagnosisMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void diagnosisCompleted(Diagnosis diagnosis, Duration elapsed) {
        try {
            String group = diagnosis.profile().productGroup().name();

            Timer.builder(PREFIX + ".duration")
                    .description("진단 한 건의 처리 시간")
                    .tag("productGroup", group)
                    .tag("degraded", String.valueOf(diagnosis.degraded().any()))
                    .publishPercentiles(0.5, 0.95)
                    .register(registry)
                    .record(elapsed);

            registry.counter(PREFIX + ".completed",
                    "productGroup", group,
                    "status", diagnosis.status().name()).increment();

            if (diagnosis.degraded().isEvidenceDegraded()) {
                registry.counter(PREFIX + ".degraded",
                        "kind", "evidence", "productGroup", group).increment();
            }
            if (diagnosis.degraded().isNarrationDegraded()) {
                registry.counter(PREFIX + ".degraded",
                        "kind", "narration", "productGroup", group).increment();
            }
            if (diagnosis.candidates().isEmpty()) {
                registry.counter(PREFIX + ".no_candidate", "productGroup", group).increment();
            }

            // 누락·확인 중 서류의 분포. "필수 서류 누락 탐지율"을 사후에 계산할 근거가 된다.
            registry.summary(PREFIX + ".documents.absent", "productGroup", group)
                    .record(count(diagnosis, ChecklistItem::isAbsent));
            registry.summary(PREFIX + ".documents.unknown", "productGroup", group)
                    .record(count(diagnosis, ChecklistItem::isUnknown));
        } catch (RuntimeException ignored) {
            // 지표 때문에 진단이 실패해서는 안 된다.
        }
    }

    @Override
    public void diagnosisFailed(String productGroup, String reason) {
        try {
            registry.counter(PREFIX + ".failed",
                    "productGroup", productGroup == null ? "UNKNOWN" : productGroup,
                    "reason", reason).increment();
        } catch (RuntimeException ignored) {
            // 위와 같은 이유
        }
    }

    @Override
    public void externalCall(String target, Duration elapsed, boolean success) {
        try {
            Timer.builder(PREFIX + ".external")
                    .description("AI 워커 호출 지연")
                    .tag("target", target)
                    .tag("success", String.valueOf(success))
                    .register(registry)
                    .record(elapsed);
        } catch (RuntimeException ignored) {
            // 위와 같은 이유
        }
    }

    private long count(Diagnosis diagnosis, java.util.function.Predicate<ChecklistItem> filter) {
        return diagnosis.checklist().stream().filter(filter).count();
    }
}
