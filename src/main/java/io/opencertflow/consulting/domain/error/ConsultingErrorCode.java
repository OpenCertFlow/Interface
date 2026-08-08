package io.opencertflow.consulting.domain.error;

import io.opencertflow.common.domain.error.ErrorCode;
import io.opencertflow.common.domain.error.ErrorType;

/** 컨설팅 컨텍스트 고유 오류. {@code CM-CONS-<번호>}. */
public enum ConsultingErrorCode implements ErrorCode {

    /** 개인정보 수집·이용 동의 없이 상담을 신청했다. */
    PRIVACY_CONSENT_REQUIRED("OCF-CONS-001", "개인정보 수집·이용 동의가 필요합니다.", ErrorType.VALIDATION),

    /** 참조한 진단이 존재하지 않는다. */
    DIAGNOSIS_NOT_FOUND("OCF-CONS-002", "연결할 진단 결과를 찾을 수 없습니다.", ErrorType.NOT_FOUND),

    /** 리드 저장 실패. */
    LEAD_SAVE_FAILED("OCF-CONS-003", "상담 신청 저장에 실패했습니다.", ErrorType.INTERNAL),

    /** 상담을 찾을 수 없다. */
    LEAD_NOT_FOUND("OCF-CONS-004", "상담 신청을 찾을 수 없습니다.", ErrorType.NOT_FOUND),

    /** 허용되지 않은 상태 전이를 시도했다. */
    INVALID_STATUS_TRANSITION("OCF-CONS-005", "상담 상태를 그렇게 바꿀 수 없습니다.", ErrorType.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorType type;

    ConsultingErrorCode(String code, String defaultMessage, ErrorType type) {
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
