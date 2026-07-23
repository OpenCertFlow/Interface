package com.certimakers.board.application.port.in;

import com.certimakers.board.domain.model.Post;
import com.certimakers.board.domain.model.PostId;
import java.util.List;
import reactor.core.publisher.Mono;

/** 게시글 작성·수정·삭제. */
public interface PostUseCase {

    Mono<PostId> write(WritePostCommand command);

    Mono<Post> edit(EditPostCommand command);

    Mono<Void> delete(DeletePostCommand command);

    /**
     * 요청자 정보. 인증 주체에서 뽑아 내려오며, 게시판 정책 판단(관리자 전용·작성자 확인)에 쓴다.
     *
     * @param userId  비로그인이면 null
     * @param isAdmin 관리자 여부
     */
    record Requester(String userId, boolean isAdmin) {

        public static Requester anonymous() {
            return new Requester(null, false);
        }

        public boolean isAuthenticated() {
            return userId != null;
        }
    }

    record WritePostCommand(
            String boardType,
            Requester requester,
            String title,
            String body,
            boolean secret,
            List<String> attachmentFileIds) {
    }

    record EditPostCommand(
            String postId,
            Requester requester,
            String title,
            String body,
            boolean secret,
            List<String> attachmentFileIds) {
    }

    record DeletePostCommand(String postId, Requester requester) {
    }
}
