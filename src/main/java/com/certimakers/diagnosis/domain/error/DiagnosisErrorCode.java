package com.certimakers.diagnosis.domain.error;

import com.certimakers.common.domain.error.ErrorCode;
import com.certimakers.common.domain.error.ErrorType;

/** 진단 컨텍스트 고유 오류. {@code CM-DIAG-<번호>}. */
public enum DiagnosisErrorCode implements ErrorCode {

    /** 활성 룰셋을 찾지 못했다. 폴백이 없는 진단 실패 지점(503). */
    RULE_SET_NOT_FOUND("CM-DIAG-001", "진단 규칙을 불러올 수 없어 잠시 진단을 제공할 수 없습니다.", ErrorType.UNAVAILABLE),

    /** 진단 ID로 결과를 찾지 못했다. */
    DIAGNOSIS_NOT_FOUND("CM-DIAG-002", "요청한 진단 결과를 찾을 수 없습니다.", ErrorType.NOT_FOUND),

    /** 애그리거트의 상태 전이 규칙 위반(예: RULE_EVALUATED 전에 근거 첨부). */
    INVALID_STATE_TRANSITION("CM-DIAG-003", "현재 진단 상태에서 수행할 수 없는 작업입니다.", ErrorType.CONFLICT),

    /** 진단 결과 저장 실패. 폴백이 없는 진단 실패 지점(500). */
    DIAGNOSIS_SAVE_FAILED("CM-DIAG-004", "진단 결과 저장에 실패했습니다.", ErrorType.INTERNAL),

    /**
     * 룰 평가가 끝나지 않은 진단으로 시뮬레이션·보완 계획을 요청했다. 점수와 체크리스트가 없으면
     * 비교 기준이 없어 "무엇이 달라지는가"를 답할 수 없다.
     */
    SIMULATION_NOT_AVAILABLE("CM-DIAG-005",
            "아직 평가가 완료되지 않은 진단은 시뮬레이션할 수 없습니다.", ErrorType.CONFLICT),

    /** 최초 진단이라 비교할 이전 진단이 없다. 재진단 결과만 비교할 수 있다(F-APP-048). */
    NOT_COMPARABLE("CM-DIAG-006",
                           "비교할 이전 진단이 없습니다. 재진단 결과만 비교할 수 있습니다.", ErrorType.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorType type;

    DiagnosisErrorCode(String code, String defaultMessage, ErrorType type) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.type = type;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public ErrorType type() {
        return type;
    }
}
