package com.certimakers.board.application.port.in;

import com.certimakers.board.application.port.in.PostUseCase.Requester;
import com.certimakers.board.domain.model.CommentId;
import reactor.core.publisher.Mono;

/** 댓글 작성·수정·삭제. */
public interface CommentUseCase {

    Mono<CommentId> write(WriteCommentCommand command);

    Mono<Void> edit(EditCommentCommand command);

    Mono<Void> delete(DeleteCommentCommand command);

    record WriteCommentCommand(String postId, Requester requester, String body) {
    }

    record EditCommentCommand(String commentId, Requester requester, String body) {
    }

    record DeleteCommentCommand(String commentId, Requester requester) {
    }
}
