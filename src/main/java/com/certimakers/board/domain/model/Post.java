package com.certimakers.board.domain.model;

import com.certimakers.board.domain.error.BoardErrorCode;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.model.AggregateRoot;
import com.certimakers.common.domain.model.Guard;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 게시글 애그리거트 루트.
 *
 * <p><b>게시판별 정책은 전부 여기서 강제한다.</b> {@link BoardType}이 규칙을 선언하고 이 애그리거트가
 * 집행한다. 컨트롤러나 서비스에 규칙을 흩어 두면 새 진입점이 생길 때마다 검증이 빠질 수 있다.
 *
 * <p>댓글은 별도 애그리거트({@link Comment})다. 글 하나에 댓글이 수백 개 달릴 수 있는데 함께 묶으면
 * 글을 열 때마다 전부 읽어야 하고, 댓글 하나 추가가 글 전체를 잠근다.
 *
 * <p>첨부는 {@code fileId} 목록으로만 참조한다. 파일 컨텍스트의 애그리거트를 물지 않는다.
 */
public class Post extends AggregateRoot<PostId> {

    private static final int MAX_ATTACHMENTS = 5;

    private final PostId id;
    private final BoardType boardType;
    private final AuthorRef author;
    private final Instant createdAt;

    private PostContent content;
    private boolean secret;
    private final List<Long> attachmentFileIds = new ArrayList<>();
    private long viewCount;
    private Instant updatedAt;

    private Post(
            PostId id, BoardType boardType, AuthorRef author, PostContent content,
            boolean secret, List<Long> attachmentFileIds, long viewCount,
            Instant createdAt, Instant updatedAt) {
        this.id = Guard.notNull(id, "id");
        this.boardType = Guard.notNull(boardType, "boardType");
        this.author = Guard.notNull(author, "author");
        this.content = Guard.notNull(content, "content");
        this.secret = secret;
        this.attachmentFileIds.addAll(attachmentFileIds);
        this.viewCount = viewCount;
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        this.updatedAt = Guard.notNull(updatedAt, "updatedAt");
    }

    /**
     * 새 글을 작성한다. 게시판 정책(작성 권한·첨부·비밀글)을 이 시점에 모두 검증한다.
     *
     * @param authorIsAdmin 작성자가 관리자인지. 관리자 전용 게시판 판단에 쓴다
     */
    public static Post write(
            PostId id,
            BoardType boardType,
            AuthorRef author,
            boolean authorIsAdmin,
            PostContent content,
            boolean secret,
            List<Long> attachmentFileIds,
            Instant now) {

        if (boardType.isAdminOnlyToWrite() && !authorIsAdmin) {
            throw new BusinessException(BoardErrorCode.ADMIN_ONLY_BOARD);
        }
        List<Long> attachments = normalizeAttachments(boardType, attachmentFileIds);
        if (secret && !boardType.allowsSecretPost()) {
            throw new BusinessException(BoardErrorCode.SECRET_POST_NOT_ALLOWED);
        }

        return new Post(id, boardType, author, content, secret, attachments, 0, now, now);
    }

    /** 저장된 상태에서 되살린다(영속성 재구성 전용). */
    public static Post reconstitute(
            PostId id, BoardType boardType, AuthorRef author, PostContent content,
            boolean secret, List<Long> attachmentFileIds, long viewCount,
            Instant createdAt, Instant updatedAt) {
        return new Post(id, boardType, author, content, secret,
                attachmentFileIds == null ? List.of() : attachmentFileIds,
                viewCount, createdAt, updatedAt);
    }

    /** 첨부 허용 여부·개수·중복을 정리한다. 중복 첨부는 조용히 하나로 접는다. */
    private static List<Long> normalizeAttachments(BoardType boardType, List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        if (!boardType.allowsAttachments()) {
            throw new BusinessException(BoardErrorCode.ATTACHMENTS_NOT_ALLOWED);
        }
        List<Long> unique = List.copyOf(new LinkedHashSet<>(raw));
        if (unique.size() > MAX_ATTACHMENTS) {
            throw new BusinessException(
                    BoardErrorCode.TOO_MANY_ATTACHMENTS,
                    "첨부는 최대 %d개까지 가능합니다.".formatted(MAX_ATTACHMENTS),
                    java.util.Map.of("max", MAX_ATTACHMENTS, "requested", unique.size()));
        }
        return unique;
    }

    /** 글을 수정한다. 작성자 본인 또는 관리자만 가능하다. */
    public void edit(
            AuthorRef editor, boolean editorIsAdmin, PostContent newContent,
            boolean newSecret, List<Long> newAttachments, Instant now) {

        requireEditableBy(editor, editorIsAdmin);
        if (newSecret && !boardType.allowsSecretPost()) {
            throw new BusinessException(BoardErrorCode.SECRET_POST_NOT_ALLOWED);
        }

        this.content = Guard.notNull(newContent, "content");
        this.secret = newSecret;
        this.attachmentFileIds.clear();
        this.attachmentFileIds.addAll(normalizeAttachments(boardType, newAttachments));
        this.updatedAt = Guard.notNull(now, "now");
    }

    /**
     * 수정·삭제 권한을 확인한다.
     *
     * <p>이 규칙이 새면 남의 글을 지울 수 있게 된다. 애그리거트에 두어 어떤 경로로 들어오든 같은
     * 판단을 거치게 한다.
     */
    public void requireEditableBy(AuthorRef requester, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return;
        }
        if (!author.equals(requester)) {
            throw new BusinessException(BoardErrorCode.NOT_POST_AUTHOR);
        }
    }

    /**
     * 열람 권한을 확인한다. 비밀글은 작성자와 관리자만 볼 수 있다.
     *
     * @param viewer 비로그인 사용자면 {@code null}
     */
    public void requireReadableBy(AuthorRef viewer, boolean viewerIsAdmin) {
        if (!secret || viewerIsAdmin) {
            return;
        }
        if (viewer == null || !author.equals(viewer)) {
            throw new BusinessException(BoardErrorCode.SECRET_POST_FORBIDDEN);
        }
    }

    /** 댓글을 받을 수 있는 게시판인지 확인한다. */
    public void requireCommentable() {
        if (!boardType.allowsComments()) {
            throw new BusinessException(BoardErrorCode.COMMENTS_NOT_ALLOWED);
        }
    }

    /** 조회수를 올린다. 목록이 아니라 상세 조회에서만 호출한다. */
    public void increaseViewCount() {
        this.viewCount++;
    }

    public boolean isAuthoredBy(AuthorRef candidate) {
        return author.equals(candidate);
    }

    @Override
    public PostId id() {
        return id;
    }

    public BoardType boardType() {
        return boardType;
    }

    public AuthorRef author() {
        return author;
    }

    public PostContent content() {
        return content;
    }

    public boolean isSecret() {
        return secret;
    }

    public List<Long> attachmentFileIds() {
        return Collections.unmodifiableList(attachmentFileIds);
    }

    public long viewCount() {
        return viewCount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
