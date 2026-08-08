package io.opencertflow.board.application.port.out;

import io.opencertflow.board.domain.model.Comment;
import io.opencertflow.board.domain.model.CommentId;
import io.opencertflow.board.domain.model.PostId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 댓글 저장·조회. 블로킹이므로 호출자는 BlockingBridge로 감싼다. */
public interface CommentRepositoryPort {

    Comment save(Comment comment);

    Optional<Comment> findById(CommentId id);

    /** 글에 달린 댓글을 작성순으로 가져온다. */
    List<Comment> findByPostId(PostId postId);

    /**
     * 여러 글의 댓글 수를 한 번에 센다.
     *
     * <p>목록 화면에서 글마다 댓글을 조회하면 N+1이 되고, 개수만 필요한데 본문까지 읽어 온다.
     * 댓글이 없는 글은 결과 맵에서 빠지므로 호출자가 0으로 채운다.
     */
    Map<Long, Integer> countByPostIds(Collection<PostId> postIds);

    void deleteById(CommentId id);

    /** 글이 지워질 때 딸린 댓글도 함께 정리한다. */
    void deleteByPostId(PostId postId);
}
