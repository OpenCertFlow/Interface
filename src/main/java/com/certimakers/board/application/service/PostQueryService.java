package com.certimakers.board.application.service;

import com.certimakers.board.application.port.in.PostQuery;
import com.certimakers.board.application.port.in.PostUseCase.Requester;
import com.certimakers.board.application.port.out.CommentRepositoryPort;
import com.certimakers.board.application.port.out.LoadAttachmentPort;
import com.certimakers.board.application.port.out.LoadAuthorNamePort;
import com.certimakers.board.application.port.out.PostRepositoryPort;
import com.certimakers.board.domain.error.BoardErrorCode;
import com.certimakers.board.domain.model.AuthorRef;
import com.certimakers.board.domain.model.BoardType;
import com.certimakers.board.domain.model.Comment;
import com.certimakers.board.domain.model.Post;
import com.certimakers.board.domain.model.PostId;
import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 게시글 조회 오케스트레이션.
 *
 * <p>두 가지가 이 클래스의 핵심이다.
 *
 * <p><b>1. 비밀글 가리기.</b> 목록에서 비밀글을 지우지 않고 제목만 가린다. 글이 있다는 사실까지
 * 숨기면 번호가 건너뛰어 게시판이 이상해 보인다. 본문은 상세 조회에서 애그리거트가 막는다.
 *
 * <p><b>2. N+1 방지.</b> 작성자 닉네임과 댓글 수를 글마다 조회하면 20개 목록에 41번 질의가 나간다.
 * 식별자를 모아 한 번에 가져온다.
 */
@UseCase
public class PostQueryService implements PostQuery {

    private static final String MASKED_TITLE = "비밀글입니다.";
    private static final String UNKNOWN_AUTHOR = "(탈퇴한 사용자)";
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepositoryPort postRepository;
    private final CommentRepositoryPort commentRepository;
    private final LoadAuthorNamePort loadAuthorNamePort;
    private final LoadAttachmentPort loadAttachmentPort;
    private final BlockingBridge blockingBridge;

    public PostQueryService(
            PostRepositoryPort postRepository,
            CommentRepositoryPort commentRepository,
            LoadAuthorNamePort loadAuthorNamePort,
            LoadAttachmentPort loadAttachmentPort,
            BlockingBridge blockingBridge) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.loadAuthorNamePort = loadAuthorNamePort;
        this.loadAttachmentPort = loadAttachmentPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<PostPage> list(String rawBoardType, int page, int size, Requester requester) {
        BoardType boardType = BoardTypes.parse(rawBoardType);
        int safePage = Math.max(page, 0);
        // 상한을 두지 않으면 size=100000 한 번으로 서버 메모리를 태울 수 있다.
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return blockingBridge.mono(() -> {
            List<Post> posts = postRepository.findByBoardType(boardType, safePage, safeSize);
            long total = postRepository.countByBoardType(boardType);

            // 닉네임과 댓글 수를 각각 한 번씩만 조회한다. 글마다 조회하면 20개 목록에 41번 질의가 나간다.
            Map<UUID, String> nicknames = loadNicknames(posts);
            Map<UUID, Integer> commentCounts = commentRepository.countByPostIds(
                    posts.stream().map(Post::id).toList());

            List<PostSummary> summaries = posts.stream()
                    .map(post -> toSummary(post, nicknames, commentCounts, requester))
                    .toList();
            return new PostPage(summaries, safePage, safeSize, total);
        });
    }

    @Override
    public Mono<PostDetail> get(String rawPostId, Requester requester) {
        PostId postId = PostIds.parse(rawPostId);
        AuthorRef viewer = requester.isAuthenticated() ? AuthorRef.of(requester.userId()) : null;

        return blockingBridge.mono(() -> {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(BoardErrorCode.POST_NOT_FOUND));
            post.requireReadableBy(viewer, requester.isAdmin());

            post.increaseViewCount();
            postRepository.save(post);

            List<Comment> comments = commentRepository.findByPostId(postId);
            Map<UUID, String> nicknames = loadNicknamesFor(post, comments);

            return new PostDetail(
                    post,
                    nicknames.getOrDefault(post.author().value(), UNKNOWN_AUTHOR),
                    loadAttachmentPort.findAll(post.attachmentFileIds()),
                    comments.stream().map(comment -> toCommentView(comment, nicknames, viewer, requester)).toList(),
                    isEditable(post.author(), viewer, requester));
        });
    }

    private PostSummary toSummary(
            Post post,
            Map<UUID, String> nicknames,
            Map<UUID, Integer> commentCounts,
            Requester requester) {

        AuthorRef viewer = requester.isAuthenticated() ? AuthorRef.of(requester.userId()) : null;
        boolean readable = !post.isSecret()
                || requester.isAdmin()
                || (viewer != null && post.isAuthoredBy(viewer));

        return new PostSummary(
                post.id().value().toString(),
                readable ? post.content().title() : MASKED_TITLE,
                nicknames.getOrDefault(post.author().value(), UNKNOWN_AUTHOR),
                post.isSecret(),
                readable,
                post.viewCount(),
                commentCounts.getOrDefault(post.id().value(), 0),
                post.createdAt().toString());
    }

    private CommentView toCommentView(
            Comment comment, Map<UUID, String> nicknames, AuthorRef viewer, Requester requester) {
        return new CommentView(
                comment.id().value().toString(),
                nicknames.getOrDefault(comment.author().value(), UNKNOWN_AUTHOR),
                comment.body(),
                isEditable(comment.author(), viewer, requester),
                comment.createdAt().toString());
    }

    private boolean isEditable(AuthorRef author, AuthorRef viewer, Requester requester) {
        return requester.isAdmin() || (viewer != null && author.equals(viewer));
    }

    /** 목록의 작성자 식별자를 모아 한 번에 조회한다. 글마다 조회하면 N+1이 된다. */
    private Map<UUID, String> loadNicknames(List<Post> posts) {
        Set<UUID> authorIds = posts.stream()
                .map(post -> post.author().value())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return loadAuthorNamePort.findNicknames(authorIds);
    }

    /** 상세 화면의 글 작성자와 댓글 작성자를 한 번에 조회한다. */
    private Map<UUID, String> loadNicknamesFor(Post post, List<Comment> comments) {
        Set<UUID> authorIds = new LinkedHashSet<>();
        authorIds.add(post.author().value());
        comments.forEach(comment -> authorIds.add(comment.author().value()));
        return loadAuthorNamePort.findNicknames(authorIds);
    }
}
