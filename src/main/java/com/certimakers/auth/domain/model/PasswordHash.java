package com.certimakers.auth.domain.model;

import com.certimakers.common.domain.model.Guard;

/**
 * 해시된 비밀번호 값 객체. <b>평문 비밀번호는 도메인에 절대 들어오지 않는다.</b>
 *
 * <p>해싱은 인프라 관심사(BCrypt)이므로 어댑터가 수행하고, 도메인은 그 결과 문자열만 다룬다.
 * 이 값 객체가 존재하는 이유는 "해시된 값"과 "평문"을 타입으로 구분하기 위함이다 — 평문 String이
 * 실수로 저장되는 것을 컴파일 단계에서 막는다.
 *
 * <p>소셜 로그인(카카오) 사용자는 비밀번호가 없다. 그 경우 이 객체 자체를 갖지 않으며,
 * {@link User}가 {@code null}로 표현한다.
 */
public record PasswordHash(String value) {

    public PasswordHash {
        Guard.hasText(value, "passwordHash");
    }

    public static PasswordHash of(String hashedValue) {
        return new PasswordHash(hashedValue);
    }
}
