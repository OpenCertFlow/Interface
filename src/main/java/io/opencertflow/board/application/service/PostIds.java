package io.opencertflow.board.application.service;

import io.opencertflow.board.domain.error.BoardErrorCode;
import io.opencertflow.board.domain.model.CommentId;
import io.opencertflow.board.domain.model.PostId;
import io.opencertflow.common.domain.error.BusinessException;

/**
 * 게시글·댓글 식별자 파싱.
 *
 * <p>형식이 깨진 식별자를 400이 아니라 <b>404로 다루는</b> 이유는, 사용자 입장에서 "잘못된 주소"와
 * "없는 글"이 구분되지 않기 때문이다. 형식 오류를 400으로 돌려주면 식별자 형식을 알려 주는 셈이 된다.
 */
final class PostIds {

    private PostIds() {
    }

    static PostId parse(String raw) {
        return PostId.of(toUuid(raw, BoardErrorCode.POST_NOT_FOUND));
    }

    static CommentId parseComment(String raw) {
        return CommentId.of(toUuid(raw, BoardErrorCode.COMMENT_NOT_FOUND));
    }

    private static Long toUuid(String raw, BoardErrorCode notFound) {
        try {
            return Long.parseLong(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(notFound);
        }
    }
}
