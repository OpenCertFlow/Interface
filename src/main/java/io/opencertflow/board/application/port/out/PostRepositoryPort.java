package io.opencertflow.board.application.port.out;

import io.opencertflow.board.domain.model.BoardType;
import io.opencertflow.board.domain.model.Post;
import io.opencertflow.board.domain.model.PostId;
import java.util.List;
import java.util.Optional;

/** 게시글 저장·조회. 블로킹이므로 호출자는 BlockingBridge로 감싼다. */
public interface PostRepositoryPort {

    Post save(Post post);

    Optional<Post> findById(PostId id);

    /**
     * 게시판별 목록을 최신순으로 가져온다.
     *
     * <p>페이지네이션을 오프셋이 아니라 {@code page}·{@code size}로 받는 이유는 해커톤 규모에서
     * 충분하고 클라이언트가 다루기 쉽기 때문이다. 게시글이 수십만 건이 되면 커서 방식으로 바꾼다.
     */
    List<Post> findByBoardType(BoardType boardType, int page, int size);

    long countByBoardType(BoardType boardType);

    void deleteById(PostId id);
}
