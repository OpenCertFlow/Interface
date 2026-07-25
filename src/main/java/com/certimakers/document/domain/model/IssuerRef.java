package com.certimakers.document.domain.model;

import com.certimakers.common.domain.model.Guard;

/** 문서를 발급한 사용자에 대한 참조. 인증 컨텍스트를 식별자로만 가리킨다. */
public record IssuerRef(Long value) {

    public IssuerRef {
        Guard.notNull(value, "issuerId");
    }

    public static IssuerRef of(Long value) {
        return new IssuerRef(value);
    }

    public static IssuerRef of(String value) {
        return new IssuerRef(Long.parseLong(value));
    }
}
