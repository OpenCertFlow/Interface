package io.opencertflow.board.adapter.in.web;

import io.opencertflow.board.application.port.in.PostQuery.PostDetail;
import io.opencertflow.board.application.port.in.PostQuery.PostPage;
import io.opencertflow.board.domain.model.BoardType;
import java.util.Arrays;
import java.util.List;

/** 게시판 API 응답 DTO. */
public final class BoardResponses {

    private BoardResponses() {
    }

    /**
     * 게시판 종류와 그 정책. 클라이언트가 <b>화면을 정책에 맞게 그리도록</b> 내려보낸다 —
     * 댓글을 못 다는 게시판에서 입력창을 감추고, 관리자 전용 게시판에서 글쓰기 버튼을 숨기려면
     * 서버의 규칙을 알아야 한다. 규칙을 앱에 하드코딩하면 서버와 어긋난다.
     */
    public record BoardTypeView(
            String code,
            String displayName,
            boolean adminOnlyToWrite,
            boolean allowsComments,
            boolean allowsAttachments,
            boolean allowsSecretPost) {

        public static List<BoardTypeView> all() {
            return Arrays.stream(BoardType.values())
                    .map(type -> new BoardTypeView(
                            type.name(),
                            type.displayName(),
                            type.isAdminOnlyToWrite(),
                            type.allowsComments(),
                            type.allowsAttachments(),
                            type.allowsSecretPost()))
                    .toList();
        }
    }

    public record PostCreated(String postId) {
    }

    public record CommentCreated(String commentId) {
    }

    /** 목록 응답. 서비스가 만든 페이지를 그대로 노출한다. */
    public record PostList(PostPage page) {
    }

    /**
     * 상세 응답. 도메인 애그리거트를 그대로 내보내지 않고 필요한 필드만 편다 —
     * 애그리거트를 직렬화하면 내부 구조 변경이 곧 API 변경이 된다.
     */
    public record PostView(
            String postId,
            String boardType,
            String title,
            String body,
            String authorNickname,
            boolean secret,
            long viewCount,
            boolean editable,
            List<AttachmentView> attachments,
            List<CommentItem> comments,
            String createdAt,
            String updatedAt) {

        public static PostView from(PostDetail detail) {
            var post = detail.post();
            return new PostView(
                    post.id().value().toString(),
                    post.boardType().name(),
                    post.content().title(),
                    post.content().body(),
                    detail.authorNickname(),
                    post.isSecret(),
                    post.viewCount(),
                    detail.editable(),
                    detail.attachments().stream()
                            .map(attachment -> new AttachmentView(
                                    attachment.fileId(),
                                    attachment.originalName(),
                                    attachment.contentType(),
                                    attachment.sizeInBytes(),
                                    attachment.downloadUrl()))
                            .toList(),
                    detail.comments().stream()
                            .map(comment -> new CommentItem(
                                    comment.commentId(),
                                    comment.authorNickname(),
                                    comment.body(),
                                    comment.editable(),
                                    comment.createdAt()))
                            .toList(),
                    post.createdAt().toString(),
                    post.updatedAt().toString());
        }
    }

    public record AttachmentView(
            String fileId,
            String originalName,
            String contentType,
            long sizeInBytes,
            String downloadUrl) {
    }

    public record CommentItem(
            String commentId,
            String authorNickname,
            String body,
            boolean editable,
            String createdAt) {
    }
}
