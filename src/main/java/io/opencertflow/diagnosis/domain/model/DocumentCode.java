package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.model.Guard;

/**
 * 서류 종류의 안정적 식별자. 예: {@code BIZ_LICENSE}, {@code CIRCUIT_DIAGRAM}, {@code TEST_REPORT}.
 *
 * <p>{@code String} 대신 이 래퍼를 쓰는 이유는, 서류 코드와 인증 제도 코드와 룰 코드가 전부
 * 문자열이라 섞이기 쉽기 때문이다. 타입이 다르면 컴파일러가 뒤바뀐 인자를 잡는다.
 */
public record DocumentCode(String value) {

    public DocumentCode {
        Guard.hasText(value, "documentCode");
    }

    public static DocumentCode of(String value) {
        return new DocumentCode(value);
    }
}
