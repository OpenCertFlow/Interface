package com.certimakers.common.domain.error;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 도메인 규칙 위반 또는 처리 불가 상황. 예상된 실패다.
 *
 * <p>{@code details}에는 클라이언트가 화면에 쓸 수 있는 구조화된 정보만 담는다. 내부 구현 세부사항
 * (SQL, 예외 클래스명, 파일 경로)은 절대 넣지 않는다. 이 맵은 그대로 응답 본문에 실린다.
 */
public class BusinessException extends RuntimeException {

    /**
     * 이 유형들은 원인 규명이 필요하므로 스택 트레이스를 채운다. 나머지(검증 실패, 404 등)는
     * 흔하고 자명하므로 {@code fillInStackTrace()} 비용을 지불하지 않는다.
     */
    private static final Set<ErrorType> STACK_TRACE_WORTHY =
            EnumSet.of(ErrorType.INTERNAL, ErrorType.EXTERNAL_SERVICE, ErrorType.TIMEOUT, ErrorType.UNAVAILABLE);

    private final transient ErrorCode errorCode;
    private final transient Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), Map.of(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of(), null);
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
        this(errorCode, message, details, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(message, cause, false, STACK_TRACE_WORTHY.contains(errorCode.type()));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static BusinessException notFound(String resource, Object id) {
        return new BusinessException(
                CommonErrorCode.RESOURCE_NOT_FOUND,
                CommonErrorCode.RESOURCE_NOT_FOUND.defaultMessage(),
                Map.of("resource", resource, "id", String.valueOf(id)));
    }

    public static BusinessException invalid(String message) {
        return new BusinessException(CommonErrorCode.INVALID_REQUEST, message);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> details() {
        return details;
    }

    public ErrorType type() {
        return errorCode.type();
    }
}
