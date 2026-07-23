package com.certimakers.board.domain.model;

import com.certimakers.board.domain.error.BoardErrorCode;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.model.AggregateRoot;
import com.certimakers.common.domain.model.Guard;
import java.time.Instant;

/**
 * 댓글 애그리거트 루트. {@link Post}와 <b>별도 애그리거트</b>이며 {@link PostId}로 참조만 한다.
 *
 * <p>글에 묶지 않은 이유는 두 가지다. 글 하나에 댓글이 수백 개 달릴 수 있는데 함께 로딩하면 상세
 * 조회가 무거워지고, 댓글 하나를 추가할 때마다 글 애그리거트 전체를 잠그게 된다.
 */
public class Comment extends AggregateRoot<CommentId> {

    private static final int BODY_MAX = 2_000;

    private final CommentId id;
    private final PostId postId;
    private final AuthorRef author;
    private final Instant createdAt;

    private String body;
    private Instant updatedAt;

    private Comment(
            CommentId id, PostId postId, AuthorRef author, String body,
            Instant createdAt, Instant updatedAt) {
        this.id = Guard.notNull(id, "id");
        this.postId = Guard.notNull(postId, "postId");
        this.author = Guard.notNull(author, "author");
        this.body = validateBody(body);
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        this.updatedAt = Guard.notNull(updatedAt, "updatedAt");
    }

    public static Comment write(
            CommentId id, PostId postId, AuthorRef author, String body, Instant now) {
        return new Comment(id, postId, author, body, now, now);
    }

    /** 저장된 상태에서 되살린다(영속성 재구성 전용). */
    public static Comment reconstitute(
            CommentId id, PostId postId, AuthorRef author, String body,
            Instant createdAt, Instant updatedAt) {
        return new Comment(id, postId, author, body, createdAt, updatedAt);
    }

    private static String validateBody(String body) {
        if (body == null || body.isBlank()) {
            throw BusinessException.invalid("댓글 내용을 입력해 주세요.");
        }
        String trimmed = body.strip();
        if (trimmed.length() > BODY_MAX) {
            throw BusinessException.invalid("댓글은 %d자 이하로 입력해 주세요.".formatted(BODY_MAX));
        }
        return trimmed;
    }

    public void edit(AuthorRef editor, boolean editorIsAdmin, String newBody, Instant now) {
        requireEditableBy(editor, editorIsAdmin);
        this.body = validateBody(newBody);
        this.updatedAt = Guard.notNull(now, "now");
    }

    public void requireEditableBy(AuthorRef requester, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return;
        }
        if (!author.equals(requester)) {
            throw new BusinessException(BoardErrorCode.NOT_COMMENT_AUTHOR);
        }
    }

    @Override
    public CommentId id() {
        return id;
    }

    public PostId postId() {
        return postId;
    }

    public AuthorRef author() {
        return author;
    }

    public String body() {
        return body;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
