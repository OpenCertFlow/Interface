package io.opencertflow.diagnosis.adapter.in.web;

import io.opencertflow.diagnosis.application.port.in.DiagnosisHistoryEntry;
import io.opencertflow.diagnosis.domain.model.DiagnosisSummary;
import io.opencertflow.diagnosis.domain.model.PrepPlan;
import java.time.Instant;

/**
 * 진단 이력 목록 응답의 한 줄(F-APP-032).
 *
 * @param prepCompleted 준비 완료 건수. 트래커(F-APP-049)를 만들지 않았으면 null
 * @param prepTotal     준비 대상 건수
 * @param prepProgress  준비 진행률(%)
 */
public record DiagnosisSummaryResponse(
        String id,
        String productName,
        String productGroup,
        String status,
        Integer readinessScore,
        boolean scoreApplicable,
        Instant createdAt,
        String previousDiagnosisId,
        Integer prepCompleted,
        Integer prepTotal,
        Integer prepProgress) {

    /**
     * 트래커를 만들지 않은 진단은 prep* 세 필드가 null이라 JSON에서 <b>키 자체가 빠진다</b>
     * ({@code default-property-inclusion: non_null}). 앱은 키 유무로 "준비 시작 전"을 판별한다 —
     * 0을 내려보내면 "시작했는데 0건 완료"와 구별되지 않는다.
     */
    public static DiagnosisSummaryResponse from(DiagnosisHistoryEntry entry) {
        DiagnosisSummary summary = entry.summary();
        PrepPlan plan = entry.prepPlan();
        return new DiagnosisSummaryResponse(
                Long.toString(summary.id()),
                summary.productName(),
                summary.productGroup().name(),
                summary.status().name(),
                summary.readinessScore(),
                summary.scoreApplicable(),
                summary.createdAt(),
                summary.previousDiagnosisId() == null
                        ? null : Long.toString(summary.previousDiagnosisId()),
                plan == null ? null : plan.completed(),
                plan == null ? null : plan.total(),
                plan == null ? null : plan.progress());
    }
}
