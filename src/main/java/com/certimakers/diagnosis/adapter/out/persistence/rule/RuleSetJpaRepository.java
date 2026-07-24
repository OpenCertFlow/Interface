package com.certimakers.diagnosis.adapter.out.persistence.rule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RuleSetJpaRepository extends JpaRepository<RuleSetEntity, UUID> {

    /** 제품군의 활성 룰셋과 룰을 함께 로드한다. 부분 유니크 인덱스가 활성 룰셋 유일성을 보장한다. */
    @EntityGraph(attributePaths = "rules")
    Optional<RuleSetEntity> findByProductGroupAndActiveIsTrue(String productGroup);

    /** 관리 화면용: 제품군·버전 내림차순 전체 조회(룰 포함). 룰셋 수가 적어 전체 로드로 충분하다. */
    @EntityGraph(attributePaths = "rules")
    List<RuleSetEntity> findAllByOrderByProductGroupAscVersionDesc();

    /** 상세 조회(룰 포함). */
    @EntityGraph(attributePaths = "rules")
    Optional<RuleSetEntity> findWithRulesById(UUID id);

    /** 제품군의 현재 최대 버전. 없으면 비어 있음 → 다음 버전은 1. */
    @Query("select max(r.version) from RuleSetEntity r where r.productGroup = :productGroup")
    Optional<Integer> findMaxVersion(String productGroup);
}
