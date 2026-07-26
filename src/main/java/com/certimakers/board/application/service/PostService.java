package com.certimakers.board.application.service;

import com.certimakers.board.application.port.in.PostUseCase;
import com.certimakers.board.application.port.out.CommentRepositoryPort;
import com.certimakers.board.application.port.out.LoadAttachmentPort;
import com.certimakers.board.application.port.out.PostRepositoryPort;
import com.certimakers.board.application.port.out.SyncAttachmentVisibilityPort;
import com.certimakers.board.domain.error.BoardErrorCode;
import com.certimakers.board.domain.model.AuthorRef;
import com.certimakers.board.domain.model.BoardType;
import com.certimakers.board.domain.model.Post;
import com.certimakers.board.domain.model.PostContent;
import com.certimakers.board.domain.model.PostId;
import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 게시글 작성·수정·삭제 오케스트레이션.
 *
 * <p>게시판별 정책(작성 권한·첨부·비밀글)은 전부 {@link Post} 애그리거트가 강제한다. 이 서비스는
 * 입력을 도메인 타입으로 바꾸고 저장을 조율할 뿐, 규칙을 다시 판단하지 않는다 — 규칙이 두 곳에
 * 있으면 언젠가 서로 어긋난다.
 *
 * <p>예외 하나: 첨부파일 소유권 확인은 이 서비스에서 한다. file 컨텍스트에 물어봐야 하는데,
 * {@link Post}는 다른 컨텍스트를 몰라야 하기 때문이다.
 */
@UseCase
public class PostService implements PostUseCase {

    private final PostRepositoryPort postRepository;
    private final CommentRepositoryPort commentRepository;
    private final LoadAttachmentPort loadAttachmentPort;
    private final SyncAttachmentVisibilityPort syncAttachmentVisibilityPort;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public PostService(
            PostRepositoryPort postRepository,
            CommentRepositoryPort commentRepository,
            LoadAttachmentPort loadAttachmentPort,
            SyncAttachmentVisibilityPort syncAttachmentVisibilityPort,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.loadAttachmentPort = loadAttachmentPort;
        this.syncAttachmentVisibilityPort = syncAttachmentVisibilityPort;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<PostId> write(WritePostCommand command) {
        BoardType boardType = BoardTypes.parse(command.boardType());
        AuthorRef author = requireAuthor(command.requester());
        PostContent content = PostContent.of(command.title(), command.body());
        List<Long> attachments = toUuids(command.attachmentFileIds());

        return blockingBridge.mono(() -> {
            requireOwnedAttachments(attachments, author);
            Post post = Post.write(
                    PostId.of(idGenerator.nextId()),
                    boardType,
                    author,
                    command.requester().isAdmin(),
                    content,
                    command.secret(),
                    attachments,
                    timeProvider.now());
            PostId savedId = postRepository.save(post).id();
            syncAttachmentVisibilityPort.syncVisibility(attachments, author.value(), command.secret());
            return savedId;
        });
    }

    @Override
    public Mono<Post> edit(EditPostCommand command) {
        AuthorRef editor = requireAuthor(command.requester());
        PostContent content = PostContent.of(command.title(), command.body());
        List<Long> attachments = toUuids(command.attachmentFileIds());
        PostId postId = PostIds.parse(command.postId());

        return blockingBridge.mono(() -> {
            requireOwnedAttachments(attachments, editor);
            Post post = loadPost(postId);
            post.edit(editor, command.requester().isAdmin(), content,
                    command.secret(), attachments, timeProvider.now());
            Post saved = postRepository.save(post);
            syncAttachmentVisibilityPort.syncVisibility(attachments, editor.value(), command.secret());
            return saved;
        });
    }

    @Override
    public Mono<Void> delete(DeletePostCommand command) {
        AuthorRef requester = requireAuthor(command.requester());
        PostId postId = PostIds.parse(command.postId());

        return blockingBridge.run(() -> {
            Post post = loadPost(postId);
            post.requireEditableBy(requester, command.requester().isAdmin());
            // 댓글을 먼저 지운다. 글만 지우면 참조가 끊긴 댓글이 남는다.
            commentRepository.deleteByPostId(postId);
            postRepository.deleteById(postId);
        });
    }

    private Post loadPost(PostId postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(BoardErrorCode.POST_NOT_FOUND));
    }

    /** 인증되지 않은 요청은 여기서 끊는다. 시큐리티가 이미 막지만 도메인 앞에서 한 번 더 확인한다. */
    private AuthorRef requireAuthor(Requester requester) {
        if (!requester.isAuthenticated()) {
            throw new BusinessException(com.certimakers.auth.domain.error.AuthErrorCode.UNAUTHENTICATED);
        }
        return AuthorRef.of(requester.userId());
    }

    /**
     * 첨부하려는 파일이 전부 요청자 소유인지 확인한다. 관리자도 예외 없다 — 새 글 첨부는
     * 글쓴이 행위지 남의 콘텐츠를 관리하는 행위가 아니다.
     */
    private void requireOwnedAttachments(List<Long> attachments, AuthorRef author) {
        if (attachments.isEmpty()) {
            return;
        }
        List<Long> notOwned = loadAttachmentPort.findNotOwnedBy(attachments, author.value());
        if (!notOwned.isEmpty()) {
            throw new BusinessException(BoardErrorCode.ATTACHMENT_NOT_OWNED);
        }
    }

    private List<Long> toUuids(List<String> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }
        try {
            return rawIds.stream().map(Long::parseLong).toList();
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("첨부 파일 식별자 형식이 올바르지 않습니다.");
        }
    }
}
