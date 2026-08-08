package io.opencertflow.board.adapter.out.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code board_post} 스프링 데이터 리포지토리. */
public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {

    List<PostEntity> findByBoardTypeOrderByCreatedAtDesc(String boardType, Pageable pageable);

    long countByBoardType(String boardType);
}
