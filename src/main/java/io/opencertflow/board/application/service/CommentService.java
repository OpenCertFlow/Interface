package io.opencertflow.board.application.service;

import io.opencertflow.auth.domain.error.AuthErrorCode;
import io.opencertflow.board.application.port.in.CommentUseCase;
import io.opencertflow.board.application.port.in.PostUseCase.Requester;
import io.opencertflow.board.application.port.out.CommentRepositoryPort;
import io.opencertflow.board.application.port.out.PostRepositoryPort;
import io.opencertflow.board.domain.error.BoardErrorCode;
import io.opencertflow.board.domain.model.AuthorRef;
import io.opencertflow.board.domain.model.Comment;
import io.opencertflow.board.domain.model.CommentId;
import io.opencertflow.board.domain.model.Post;
import io.opencertflow.board.domain.model.PostId;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.common.domain.port.TimeProvider;
import reactor.core.publisher.Mono;

/**
 * 댓글 작성·수정·삭제 오케스트레이션.
 *
 * <p>댓글을 달기 전에 <b>글이 있는지</b>와 <b>그 게시판이 댓글을 허용하는지</b>를 확인한다. 후자는
 * {@link Post#requireCommentable()}가 판단한다 — 공지·자료실처럼 댓글을 막은 게시판이 있기 때문이다.
 *
 * <p>비밀글의 댓글도 열람 권한을 따른다. 글을 볼 수 없는 사람이 댓글을 달 수 있으면 비밀글의
 * 의미가 없다.
 */
@UseCase
public class CommentService implements CommentUseCase {

    private final CommentRepositoryPort commentRepository;
    private final PostRepositoryPort postRepository;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public CommentService(
            CommentRepositoryPort commentRepository,
            PostRepositoryPort postRepository,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<CommentId> write(WriteCommentCommand command) {
        AuthorRef author = requireAuthor(command.requester());
        PostId postId = PostIds.parse(command.postId());

        return blockingBridge.mono(() -> {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(BoardErrorCode.POST_NOT_FOUND));
            post.requireCommentable();
            post.requireReadableBy(author, command.requester().isAdmin());

            Comment comment = Comment.write(
                    CommentId.of(idGenerator.nextId()), postId, author,
                    command.body(), timeProvider.now());
            return commentRepository.save(comment).id();
        });
    }

    @Override
    public Mono<Void> edit(EditCommentCommand command) {
        AuthorRef editor = requireAuthor(command.requester());
        CommentId commentId = PostIds.parseComment(command.commentId());

        return blockingBridge.run(() -> {
            Comment comment = loadComment(commentId);
            comment.edit(editor, command.requester().isAdmin(), command.body(), timeProvider.now());
            commentRepository.save(comment);
        });
    }

    @Override
    public Mono<Void> delete(DeleteCommentCommand command) {
        AuthorRef requester = requireAuthor(command.requester());
        CommentId commentId = PostIds.parseComment(command.commentId());

        return blockingBridge.run(() -> {
            Comment comment = loadComment(commentId);
            comment.requireEditableBy(requester, command.requester().isAdmin());
            commentRepository.deleteById(commentId);
        });
    }

    private Comment loadComment(CommentId commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(BoardErrorCode.COMMENT_NOT_FOUND));
    }

    private AuthorRef requireAuthor(Requester requester) {
        if (!requester.isAuthenticated()) {
            throw new BusinessException(AuthErrorCode.UNAUTHENTICATED);
        }
        return AuthorRef.of(requester.userId());
    }
}
