package com.certimakers.diagnosis.adapter.out.persistence.rule;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleSetJpaRepository extends JpaRepository<RuleSetEntity, UUID> {

    /** 제품군의 활성 룰셋과 룰을 함께 로드한다. 부분 유니크 인덱스가 활성 룰셋 유일성을 보장한다. */
    @EntityGraph(attributePaths = "rules")
    Optional<RuleSetEntity> findByProductGroupAndActiveIsTrue(String productGroup);
}
