package com.certimakers.file.domain.error;

import com.certimakers.common.domain.error.ErrorCode;
import com.certimakers.common.domain.error.ErrorType;

/** 파일 컨텍스트 고유 오류. {@code CM-FILE-<번호>}. */
public enum FileErrorCode implements ErrorCode {

    /** 요청한 파일 메타데이터가 없다. */
    FILE_NOT_FOUND("CM-FILE-001", "파일을 찾을 수 없습니다.", ErrorType.NOT_FOUND),

    /** 허용 용량을 초과했다. */
    FILE_TOO_LARGE("CM-FILE-002", "허용 용량을 초과했습니다.", ErrorType.VALIDATION),

    /** 허용하지 않는 확장자·형식이다. */
    UNSUPPORTED_FILE_TYPE("CM-FILE-003", "허용하지 않는 파일 형식입니다.", ErrorType.VALIDATION),

    /** 저장소 읽기·쓰기에 실패했다. */
    STORAGE_FAILURE("CM-FILE-004", "파일 저장에 실패했습니다.", ErrorType.INTERNAL),

    /** 본인이 올리지 않은 파일을 삭제하려 했다. */
    NOT_FILE_OWNER("CM-FILE-005", "본인이 업로드한 파일만 삭제할 수 있습니다.", ErrorType.CONFLICT),

    /** 업로드 본문이 비어 있다. */
    EMPTY_FILE("CM-FILE-006", "빈 파일은 업로드할 수 없습니다.", ErrorType.VALIDATION);

    private final String code;
    private final String defaultMessage;
    private final ErrorType type;

    FileErrorCode(String code, String defaultMessage, ErrorType type) {
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
