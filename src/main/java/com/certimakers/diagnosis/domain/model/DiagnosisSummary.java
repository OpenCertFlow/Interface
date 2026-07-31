package com.certimakers.diagnosis.domain.model;

import java.time.Instant;

/**
 * 진단 이력 목록의 한 줄(F-APP-032). 애그리거트 전체가 아니라 목록에 필요한 요약만 담는다 —
 * 목록마다 후보·체크리스트·근거를 전부 로드하면 무겁다.
 *
 * @param id             진단 식별자
 * @param productName    제품명
 * @param productGroup   제품군
 * @param status         진단 상태(COMPLETED 등)
 * @param readinessScore 준비도 점수(%). 산정 불가면 null
 * @param scoreApplicable 점수 산정이 가능했는지
 * @param createdAt      진단 시각
 * @param previousDiagnosisId 재진단이면 원 진단 id, 최초 진단이면 null.
 *                            앱이 재진단 여부를 판별해 비교 진입점을 그린다
 */
public record DiagnosisSummary(
        long id,
        String productName,
        ProductGroup productGroup,
        DiagnosisStatus status,
        Integer readinessScore,
        boolean scoreApplicable,
        Instant createdAt,
        Long previousDiagnosisId) {
}
