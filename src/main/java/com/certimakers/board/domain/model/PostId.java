package com.certimakers.board.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.UUID;

/** 게시글 식별자. 값은 UUIDv7(시간 정렬)이라 목록 정렬과 인덱스 지역성이 함께 좋아진다. */
public record PostId(UUID value) {

    public PostId {
        Guard.notNull(value, "postId");
    }

    public static PostId of(UUID value) {
        return new PostId(value);
    }
}
