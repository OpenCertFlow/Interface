package com.certimakers.auth.domain.model;

import com.certimakers.common.domain.model.Guard;

/** 사용자 식별자. 값은 전역 시퀀스에서 나오며 {@code IdGenerator} 포트가 생성한다. */
public record UserId(Long value) {

    public UserId {
        Guard.notNull(value, "userId");
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }
}
