package io.opencertflow.board.domain.model;

import io.opencertflow.common.domain.model.Guard;

/** 댓글 식별자. */
public record CommentId(Long value) {

    public CommentId {
        Guard.notNull(value, "commentId");
    }

    public static CommentId of(Long value) {
        return new CommentId(value);
    }
}
