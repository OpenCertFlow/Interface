package com.certimakers.auth.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.UUID;

/** 사용자 식별자. 값은 UUIDv7(시간 정렬)이며 {@code IdGenerator} 포트가 생성한다. */
public record UserId(UUID value) {

    public UserId {
        Guard.notNull(value, "userId");
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }
}
