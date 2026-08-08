package io.opencertflow.document.domain.error;

import io.opencertflow.common.domain.error.ErrorCode;
import io.opencertflow.common.domain.error.ErrorType;

/** 문서 발급 컨텍스트 고유 오류. {@code CM-DOC-<번호>}. */
public enum DocumentErrorCode implements ErrorCode {

    /** 존재하지 않는 양식 코드다. */
    TEMPLATE_NOT_FOUND("OCF-DOC-001", "요청한 문서 양식을 찾을 수 없습니다.", ErrorType.NOT_FOUND),

    /** 필수 입력란이 비어 있다. */
    REQUIRED_FIELD_MISSING("OCF-DOC-002", "필수 항목이 입력되지 않았습니다.", ErrorType.VALIDATION),

    /** 양식에 없는 항목을 보냈다. */
    UNKNOWN_FIELD("OCF-DOC-003", "양식에 없는 항목이 포함되어 있습니다.", ErrorType.VALIDATION),

    /** 입력값이 형식·길이 규칙을 위반했다. */
    INVALID_FIELD_VALUE("OCF-DOC-004", "입력값 형식이 올바르지 않습니다.", ErrorType.VALIDATION),

    /** 발급 이력을 찾지 못했다. */
    ISSUED_DOCUMENT_NOT_FOUND("OCF-DOC-005", "발급 이력을 찾을 수 없습니다.", ErrorType.NOT_FOUND),

    /** 본인이 발급하지 않은 문서에 접근했다. */
    NOT_DOCUMENT_OWNER("OCF-DOC-006", "본인이 발급한 문서만 조회할 수 있습니다.", ErrorType.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorType type;

    DocumentErrorCode(String code, String defaultMessage, ErrorType type) {
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
