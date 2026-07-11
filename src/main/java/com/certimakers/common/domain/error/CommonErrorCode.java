package com.certimakers.common.domain.error;

/** 어느 컨텍스트에도 속하지 않는 공통 오류. 컨텍스트 고유 오류는 각자 enum을 만든다. */
public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST("CM-C-400", "요청 값이 올바르지 않습니다.", ErrorType.VALIDATION),
    RESOURCE_NOT_FOUND("CM-C-404", "요청한 정보를 찾을 수 없습니다.", ErrorType.NOT_FOUND),
    ILLEGAL_STATE("CM-C-409", "현재 상태에서 수행할 수 없는 요청입니다.", ErrorType.CONFLICT),

    EXTERNAL_SERVICE_ERROR("CM-C-502", "외부 서비스 호출에 실패했습니다.", ErrorType.EXTERNAL_SERVICE),
    EXTERNAL_SERVICE_TIMEOUT("CM-C-504", "외부 서비스 응답이 지연되고 있습니다.", ErrorType.TIMEOUT),
    SERVICE_UNAVAILABLE("CM-C-503", "일시적으로 서비스를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", ErrorType.UNAVAILABLE),

    INTERNAL_ERROR("CM-C-500", "처리 중 오류가 발생했습니다.", ErrorType.INTERNAL);

    private final String code;
    private final String defaultMessage;
    private final ErrorType type;

    CommonErrorCode(String code, String defaultMessage, ErrorType type) {
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
