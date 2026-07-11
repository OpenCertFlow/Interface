package com.certimakers.common.adapter.in.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * 오류 응답 본문.
 *
 * @param code        안정적인 오류 식별자. 클라이언트는 이 값으로 분기한다.
 * @param message     사용자에게 보여줄 문구. 문구는 바뀔 수 있으니 분기 조건으로 쓰지 말 것.
 * @param fieldErrors 필드 단위 검증 실패 목록.
 * @param details     화면에 쓸 수 있는 구조화된 부가 정보. 내부 구현 세부사항은 절대 담지 않는다.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> fieldErrors,
        Map<String, Object> details) {

    public record FieldError(String field, Object rejectedValue, String reason) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), Map.of());
    }

    public static ErrorResponse of(String code, String message, Map<String, Object> details) {
        return new ErrorResponse(code, message, List.of(), details);
    }

    public static ErrorResponse withFieldErrors(String code, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, fieldErrors, Map.of());
    }
}
