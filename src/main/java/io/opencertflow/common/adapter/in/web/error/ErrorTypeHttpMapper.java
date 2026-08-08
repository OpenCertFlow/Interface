package io.opencertflow.common.adapter.in.web.error;

import io.opencertflow.common.domain.error.ErrorType;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * 도메인의 {@link ErrorType}을 HTTP 상태 코드로 옮긴다.
 *
 * <p>이 매핑이 웹 어댑터에 있는 이유는 도메인이 HTTP를 몰라야 하기 때문이다. 같은 도메인이
 * gRPC나 메시지 컨슈머로 노출된다면 다른 매퍼가 붙는다.
 */
public final class ErrorTypeHttpMapper {

    private static final Map<ErrorType, HttpStatus> MAPPING = new EnumMap<>(ErrorType.class);

    static {
        MAPPING.put(ErrorType.VALIDATION, HttpStatus.BAD_REQUEST);
        MAPPING.put(ErrorType.NOT_FOUND, HttpStatus.NOT_FOUND);
        MAPPING.put(ErrorType.CONFLICT, HttpStatus.CONFLICT);
        MAPPING.put(ErrorType.EXTERNAL_SERVICE, HttpStatus.BAD_GATEWAY);
        MAPPING.put(ErrorType.TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
        MAPPING.put(ErrorType.UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        MAPPING.put(ErrorType.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS);
        MAPPING.put(ErrorType.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorTypeHttpMapper() {
    }

    public static HttpStatus toStatus(ErrorType type) {
        return MAPPING.getOrDefault(type, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
