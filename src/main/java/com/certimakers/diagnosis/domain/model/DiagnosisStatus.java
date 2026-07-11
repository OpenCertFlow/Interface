package com.certimakers.diagnosis.domain.model;

/**
 * 진단의 생애주기 상태. 전이 규칙은 03-diagnosis-flow.md의 상태 다이어그램을 따른다.
 *
 * <p>핵심은 {@link #COMPLETED_DEGRADED}가 실패가 아니라는 점이다. 판정과 점수는 COMPLETED와
 * 동일하게 유효하며, 근거 또는 설명 문장만 결여된 상태다.
 */
public enum DiagnosisStatus {

    /** 요청 접수. 아직 아무것도 평가되지 않았다. */
    REQUESTED,

    /** 룰 평가와 점수 산정 완료. 이 시점에 판정은 확정된다. */
    RULE_EVALUATED,

    /** 근거·문장화까지 성공. 온전한 리포트. */
    COMPLETED,

    /** 근거 또는 문장화가 실패했으나 판정·점수는 유효. 배너로 사용자에게 알린다. */
    COMPLETED_DEGRADED,

    /** 룰셋 로드 실패 또는 저장 실패. 폴백이 없는 유일한 두 경우. */
    FAILED;

    public boolean isTerminalSuccess() {
        return this == COMPLETED || this == COMPLETED_DEGRADED;
    }
}
