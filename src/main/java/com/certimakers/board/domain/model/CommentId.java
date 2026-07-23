package com.certimakers.board.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.UUID;

/** 댓글 식별자. */
public record CommentId(UUID value) {

    public CommentId {
        Guard.notNull(value, "commentId");
    }

    public static CommentId of(UUID value) {
        return new CommentId(value);
    }
}
