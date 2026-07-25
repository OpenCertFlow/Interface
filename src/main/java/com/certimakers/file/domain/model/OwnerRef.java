package com.certimakers.file.domain.model;

import com.certimakers.common.domain.model.Guard;

/**
 * 파일을 올린 사용자에 대한 참조.
 *
 * <p>인증 컨텍스트의 {@code UserId}를 직접 참조하지 않고 <b>식별자만</b> 들고 있다. 다른 바운디드
 * 컨텍스트의 애그리거트를 객체로 물면 두 컨텍스트가 한 덩어리로 묶여, 한쪽 변경이 다른 쪽을 잠근다.
 */
public record OwnerRef(Long value) {

    public OwnerRef {
        Guard.notNull(value, "ownerId");
    }

    public static OwnerRef of(Long value) {
        return new OwnerRef(value);
    }

    public static OwnerRef of(String value) {
        return new OwnerRef(Long.parseLong(value));
    }
}
