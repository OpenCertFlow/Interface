package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;

/**
 * 인증 제도의 안정적 식별자. 예: {@code KC_SAFETY_CONFIRM_ELECTRIC}.
 *
 * <p>인증 후보를 식별할 때 {@link CertificationType}과 함께 쓰인다. 유형은 세 갈래지만 제도는
 * 제품군·근거 문서에 따라 더 세분화되므로 별도 코드로 둔다.
 */
public record SchemeCode(String value) {

    public SchemeCode {
        Guard.hasText(value, "schemeCode");
    }

    public static SchemeCode of(String value) {
        return new SchemeCode(value);
    }
}
