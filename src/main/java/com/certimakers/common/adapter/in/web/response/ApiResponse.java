package com.certimakers.common.adapter.in.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * 모든 API 응답을 감싸는 봉투.
 *
 * <p>Android 클라이언트가 성공/실패 분기를 한 곳에서 처리하고, 장애 시 사용자가 화면의
 * {@code traceId}를 그대로 읽어 주면 서버 로그를 찾을 수 있게 하기 위함이다.
 *
 * <pre>{@code
 * // 성공
 * { "success": true, "data": { ... }, "traceId": "0198...", "timestamp": "2026-07-10T04:00:00Z" }
 * // 실패
 * { "success": false, "error": { "code": "CM-DIAG-001", "message": "..." }, "traceId": "0198...", ... }
 * }</pre>
 *
 * @param <T> 성공 시 페이로드 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error,
        String traceId,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data, String traceId, Instant timestamp) {
        return new ApiResponse<>(true, data, null, traceId, timestamp);
    }

    public static <T> ApiResponse<T> failure(ErrorResponse error, String traceId, Instant timestamp) {
        return new ApiResponse<>(false, null, error, traceId, timestamp);
    }
}
