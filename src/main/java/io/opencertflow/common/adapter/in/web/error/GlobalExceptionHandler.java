package io.opencertflow.common.adapter.in.web.error;

import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.response.ErrorResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.error.CommonErrorCode;
import io.opencertflow.common.domain.error.ErrorCode;
import io.opencertflow.common.domain.port.TimeProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * 모든 예외를 {@link ApiResponse} 봉투로 변환하는 단일 지점.
 *
 * <p>여기서 스택 트레이스나 예외 클래스명이 응답 본문으로 새어 나가지 않도록 한다. 5xx의 원인은
 * 서버 로그에만 남기고, 클라이언트에는 traceId만 준다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final TimeProvider timeProvider;

    public GlobalExceptionHandler(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBusiness(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        HttpStatus status = ErrorTypeHttpMapper.toStatus(exception.type());
        ErrorResponse error =
                ErrorResponse.of(errorCode.code(), exception.getMessage(), exception.details());

        return respond(status, error, exception);
    }

    /** Bean Validation 실패. 어느 필드가 왜 틀렸는지 그대로 돌려준다. */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBindException(WebExchangeBindException exception) {
        List<ErrorResponse.FieldError> fieldErrors = exception.getFieldErrors().stream()
                .map(fieldError -> new ErrorResponse.FieldError(
                        fieldError.getField(),
                        fieldError.getRejectedValue(),
                        fieldError.getDefaultMessage()))
                .toList();

        ErrorResponse error = ErrorResponse.withFieldErrors(
                CommonErrorCode.INVALID_REQUEST.code(),
                CommonErrorCode.INVALID_REQUEST.defaultMessage(),
                fieldErrors);

        return respond(HttpStatus.BAD_REQUEST, error, exception);
    }

    /** 본문 파싱 실패, 타입 불일치 등. 상세 사유는 노출하지 않는다 — 내부 구조가 드러난다. */
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleInput(ServerWebInputException exception) {
        ErrorResponse error = ErrorResponse.of(
                CommonErrorCode.INVALID_REQUEST.code(), CommonErrorCode.INVALID_REQUEST.defaultMessage());

        return respond(HttpStatus.BAD_REQUEST, error, exception);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        ErrorCode errorCode = status.is4xxClientError()
                ? CommonErrorCode.INVALID_REQUEST
                : CommonErrorCode.INTERNAL_ERROR;

        return respond(status, ErrorResponse.of(errorCode.code(), errorCode.defaultMessage()), exception);
    }

    /** 여기에 도달했다는 것은 우리가 예상하지 못한 일이 일어났다는 뜻이다. 반드시 스택 트레이스를 남긴다. */
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleUnexpected(Throwable throwable) {
        ErrorResponse error = ErrorResponse.of(
                CommonErrorCode.INTERNAL_ERROR.code(), CommonErrorCode.INTERNAL_ERROR.defaultMessage());

        return respond(HttpStatus.INTERNAL_SERVER_ERROR, error, throwable);
    }

    private Mono<ResponseEntity<ApiResponse<Void>>> respond(
            HttpStatus status, ErrorResponse error, Throwable cause) {

        return TraceId.current().map(traceId -> {
            logBySeverity(status, error, traceId, cause);
            ApiResponse<Void> body = ApiResponse.failure(error, traceId, timeProvider.now());
            return ResponseEntity.status(status).body(body);
        });
    }

    private void logBySeverity(HttpStatus status, ErrorResponse error, String traceId, Throwable cause) {
        if (status.is5xxServerError()) {
            log.error("[{}] {} {} — {}", traceId, status.value(), error.code(), cause.getMessage(), cause);
        } else {
            log.warn("[{}] {} {} — {}", traceId, status.value(), error.code(), cause.getMessage());
        }
    }
}
