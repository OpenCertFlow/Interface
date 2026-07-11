package com.certimakers.common.domain.error;

import java.util.Map;

/**
 * 외부 시스템(AI 워커, LLM API) 호출 실패.
 *
 * <p>이 예외는 대개 <b>잡혀서 폴백으로 전환</b>된다. 진단 흐름에서 RAG 검색과 문장화는 실패해도
 * 진단 자체는 완료되어야 하기 때문이다. 반대로 룰셋 로드 실패처럼 폴백이 없는 경우는
 * {@link CommonErrorCode#SERVICE_UNAVAILABLE}로 승격되어 503으로 나간다.
 *
 * <p>폴백 정책 전체는 {@code docs/design/03-diagnosis-flow.md} 참조.
 */
public class ExternalSystemException extends BusinessException {

    private final transient String systemName;

    private ExternalSystemException(ErrorCode errorCode, String systemName, String message, Throwable cause) {
        super(errorCode, message, Map.of("system", systemName), cause);
        this.systemName = systemName;
    }

    public static ExternalSystemException of(String systemName, String message, Throwable cause) {
        return new ExternalSystemException(
                CommonErrorCode.EXTERNAL_SERVICE_ERROR, systemName, message, cause);
    }

    public static ExternalSystemException timeout(String systemName, Throwable cause) {
        return new ExternalSystemException(
                CommonErrorCode.EXTERNAL_SERVICE_TIMEOUT,
                systemName,
                "%s 응답이 제한 시간을 초과했습니다.".formatted(systemName),
                cause);
    }

    public String systemName() {
        return systemName;
    }
}
