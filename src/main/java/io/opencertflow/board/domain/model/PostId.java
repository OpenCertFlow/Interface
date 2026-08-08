package io.opencertflow.board.domain.model;

import io.opencertflow.common.domain.model.Guard;

/** 게시글 식별자. 값은 전역 시퀀스라 목록 정렬과 인덱스 지역성이 함께 좋아진다. */
public record PostId(Long value) {

    public PostId {
        Guard.notNull(value, "postId");
    }

    public static PostId of(Long value) {
        return new PostId(value);
    }
}
