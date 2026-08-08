package io.opencertflow.common.domain.error;

/**
 * 오류의 성격을 도메인 언어로 분류한다.
 *
 * <p>여기에 {@code HttpStatus}가 없는 것은 의도적이다. 도메인은 자신이 HTTP로 노출된다는 사실을
 * 몰라야 한다. HTTP 상태 코드로의 변환은 웹 어댑터의 책임이다.
 */
public enum ErrorType {

    /** 입력값이 규칙을 위반했다. 사용자가 고칠 수 있다. */
    VALIDATION,

    /** 요청한 리소스가 없다. */
    NOT_FOUND,

    /** 현재 상태에서 허용되지 않는 전이다. */
    CONFLICT,

    /** 외부 시스템(AI 워커, LLM)이 오류를 반환했다. */
    EXTERNAL_SERVICE,

    /** 외부 시스템이 제한 시간 내에 응답하지 않았다. */
    TIMEOUT,

    /** 필수 의존 요소(룰셋 등)를 사용할 수 없어 서비스를 제공할 수 없다. */
    UNAVAILABLE,

    /** 짧은 시간에 너무 많이 시도했다. 잠시 뒤 다시 하면 된다. */
    RATE_LIMITED,

    /** 예상하지 못한 내부 오류. 사용자가 할 수 있는 것이 없다. */
    INTERNAL
}
