package com.certimakers.board.application.port.in;

import com.certimakers.board.application.port.in.PostUseCase.Requester;
import com.certimakers.board.application.port.out.LoadAttachmentPort.AttachmentInfo;
import com.certimakers.board.domain.model.Post;
import java.util.List;
import reactor.core.publisher.Mono;

/** 게시글 조회. 목록과 상세를 나눠 제공한다. */
public interface PostQuery {

    /**
     * 게시판 목록. 비밀글은 <b>제목을 가려서</b> 내려보내되 목록에서 지우지는 않는다 —
     * 글이 있다는 사실 자체는 보여야 게시판이 자연스럽다.
     */
    Mono<PostPage> list(String boardType, int page, int size, Requester requester);

    /** 게시글 상세. 비밀글 권한을 확인하고 조회수를 올린다. */
    Mono<PostDetail> get(String postId, Requester requester);

    /**
     * @param totalCount 전체 글 수. 클라이언트가 페이지 수를 계산한다
     */
    record PostPage(List<PostSummary> posts, int page, int size, long totalCount) {
    }

    /**
     * 목록 한 줄.
     *
     * @param maskedTitle 비밀글이고 볼 권한이 없으면 가려진 제목
     * @param readable    본문을 열어 볼 수 있는지
     */
    record PostSummary(
            String postId,
            String maskedTitle,
            String authorNickname,
            boolean secret,
            boolean readable,
            long viewCount,
            int commentCount,
            String createdAt) {
    }

    record PostDetail(
            Post post,
            String authorNickname,
            List<AttachmentInfo> attachments,
            List<CommentView> comments,
            boolean editable) {
    }

    record CommentView(
            String commentId,
            String authorNickname,
            String body,
            boolean editable,
            String createdAt) {
    }
}
