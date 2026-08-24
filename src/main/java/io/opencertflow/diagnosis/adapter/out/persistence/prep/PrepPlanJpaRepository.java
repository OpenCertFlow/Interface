package io.opencertflow.diagnosis.adapter.out.persistence.prep;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrepPlanJpaRepository extends JpaRepository<PrepPlanEntity, Long> {

    /** 진단당 계획은 하나다(V33의 UNIQUE 제약). */
    Optional<PrepPlanEntity> findByDiagnosisId(Long diagnosisId);

    /**
     * 여러 진단의 계획을 항목까지 한 번에 가져온다.
     *
     * <p>{@code items}가 LAZY라 fetch join이 없으면 계획 수만큼 추가 쿼리가 나간다 — 목록
     * 화면에서 N+1을 피하려고 만든 메서드인데 정작 항목에서 N+1이 나면 의미가 없다.
     */
    @Query("select distinct p from PrepPlanEntity p left join fetch p.items "
            + "where p.diagnosisId in :diagnosisIds")
    List<PrepPlanEntity> findAllWithItemsByDiagnosisIdIn(
            @Param("diagnosisIds") Collection<Long> diagnosisIds);
}
