package io.opencertflow.board.adapter.in.web;

import io.opencertflow.board.application.port.in.CommentUseCase;
import io.opencertflow.board.application.port.in.PostQuery;
import io.opencertflow.board.application.port.in.PostUseCase;
import io.opencertflow.board.application.port.in.PostUseCase.Requester;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/**
 * 게시판 API. 게시판 종류를 경로 변수로 받아 <b>하나의 컨트롤러가 모든 게시판을 처리한다.</b>
 *
 * <p>게시판마다 컨트롤러를 두지 않는 이유는, 종류별 차이가 전부 {@code BoardType} 정책으로 표현되기
 * 때문이다. 새 게시판을 추가할 때 enum에 한 줄만 넣으면 API가 따라온다.
 *
 * <p>목록·상세는 비로그인도 볼 수 있고(비밀글은 가려짐), 작성·수정·삭제는 인증이 필요하다.
 */
@WebAdapter
@RequestMapping("/api/v1/boards")
public class BoardController {

    private final PostUseCase postUseCase;
    private final PostQuery postQuery;
    private final CommentUseCase commentUseCase;
    private final TimeProvider timeProvider;

    public BoardController(
            PostUseCase postUseCase,
            PostQuery postQuery,
            CommentUseCase commentUseCase,
            TimeProvider timeProvider) {
        this.postUseCase = postUseCase;
        this.postQuery = postQuery;
        this.commentUseCase = commentUseCase;
        this.timeProvider = timeProvider;
    }

    /** 게시판 종류와 정책 목록. 클라이언트가 화면을 정책에 맞게 그리는 데 쓴다. */
    @GetMapping("/types")
    public Mono<ResponseEntity<ApiResponse<List<BoardResponses.BoardTypeView>>>> boardTypes() {
        return wrap(BoardResponses.BoardTypeView.all(), HttpStatus.OK);
    }

    @GetMapping("/{boardType}/posts")
    public Mono<ResponseEntity<ApiResponse<BoardResponses.PostList>>> list(
            @PathVariable String boardType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return currentRequester()
                .flatMap(requester -> postQuery.list(boardType, page, size, requester))
                .map(BoardResponses.PostList::new)
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @GetMapping("/posts/{postId}")
    public Mono<ResponseEntity<ApiResponse<BoardResponses.PostView>>> get(
            @PathVariable String postId) {

        return currentRequester()
                .flatMap(requester -> postQuery.get(postId, requester))
                .map(BoardResponses.PostView::from)
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PostMapping("/{boardType}/posts")
    public Mono<ResponseEntity<ApiResponse<BoardResponses.PostCreated>>> write(
            @PathVariable String boardType,
            @Valid @RequestBody BoardRequests.WritePost request) {

        return currentRequester()
                .flatMap(requester -> postUseCase.write(new PostUseCase.WritePostCommand(
                        boardType, requester, request.title(), request.body(),
                        request.secret(), request.attachmentFileIds())))
                .map(postId -> new BoardResponses.PostCreated(postId.value().toString()))
                .flatMap(body -> wrap(body, HttpStatus.CREATED));
    }

    @PutMapping("/posts/{postId}")
    public Mono<ResponseEntity<ApiResponse<BoardResponses.PostCreated>>> edit(
            @PathVariable String postId,
            @Valid @RequestBody BoardRequests.WritePost request) {

        return currentRequester()
                .flatMap(requester -> postUseCase.edit(new PostUseCase.EditPostCommand(
                        postId, requester, request.title(), request.body(),
                        request.secret(), request.attachmentFileIds())))
                .map(post -> new BoardResponses.PostCreated(post.id().value().toString()))
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @DeleteMapping("/posts/{postId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable String postId) {
        return currentRequester()
                .flatMap(requester -> postUseCase.delete(
                        new PostUseCase.DeletePostCommand(postId, requester)))
                .then(wrap(null, HttpStatus.NO_CONTENT));
    }

    @PostMapping("/posts/{postId}/comments")
    public Mono<ResponseEntity<ApiResponse<BoardResponses.CommentCreated>>> writeComment(
            @PathVariable String postId,
            @Valid @RequestBody BoardRequests.WriteComment request) {

        return currentRequester()
                .flatMap(requester -> commentUseCase.write(
                        new CommentUseCase.WriteCommentCommand(postId, requester, request.body())))
                .map(commentId -> new BoardResponses.CommentCreated(commentId.value().toString()))
                .flatMap(body -> wrap(body, HttpStatus.CREATED));
    }

    @PutMapping("/comments/{commentId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> editComment(
            @PathVariable String commentId,
            @Valid @RequestBody BoardRequests.WriteComment request) {

        return currentRequester()
                .flatMap(requester -> commentUseCase.edit(
                        new CommentUseCase.EditCommentCommand(commentId, requester, request.body())))
                .then(wrap(null, HttpStatus.OK));
    }

    @DeleteMapping("/comments/{commentId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteComment(@PathVariable String commentId) {
        return currentRequester()
                .flatMap(requester -> commentUseCase.delete(
                        new CommentUseCase.DeleteCommentCommand(commentId, requester)))
                .then(wrap(null, HttpStatus.NO_CONTENT));
    }

    /**
     * 현재 요청자를 만든다. 인증되지 않았으면 익명으로 돌려준다 —
     * 목록·상세는 비로그인도 볼 수 있어야 하므로 여기서 끊지 않는다.
     */
    private Mono<Requester> currentRequester() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> toRequester(context.getAuthentication()))
                .defaultIfEmpty(Requester.anonymous());
    }

    private Requester toRequester(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Requester.anonymous();
        }
        boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        return new Requester(authentication.getName(), admin);
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
