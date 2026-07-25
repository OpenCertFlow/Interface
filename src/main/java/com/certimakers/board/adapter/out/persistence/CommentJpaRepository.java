package com.certimakers.board.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** {@code board_comment} 스프링 데이터 리포지토리. */
public interface CommentJpaRepository extends JpaRepository<CommentEntity, Long> {

    List<CommentEntity> findByPostIdOrderByCreatedAtAsc(Long postId);

    /**
     * 여러 글의 댓글 수를 한 번의 질의로 센다.
     *
     * <p>{@code (postId, count)} 튜플로 받아 호출자가 맵으로 만든다. 글마다 조회하면 N+1이 된다.
     */
    @Query("""
            SELECT c.postId, COUNT(c)
            FROM CommentEntity c
            WHERE c.postId IN :postIds
            GROUP BY c.postId
            """)
    List<Object[]> countGroupedByPostId(@Param("postIds") Collection<Long> postIds);

    void deleteByPostId(Long postId);
}
