package io.opencertflow.diagnosis.adapter.out.persistence.diagnosis;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 진단 애그리거트 리포지토리.
 *
 * <p>여러 {@code List} 자식을 한 번에 join fetch하면 Hibernate가 MultipleBagFetchException을
 * 던진다. 그래서 별도 fetch 그래프를 두지 않고, 로드 어댑터의 트랜잭션 안에서 각 컬렉션을
 * 지연 초기화한다. 단일 애그리거트 로드라 컬렉션이 작아 N+1 비용은 무시할 수준이다.
 */
public interface DiagnosisJpaRepository extends JpaRepository<DiagnosisEntity, Long> {

    /** 소유자의 진단을 최신순으로. 진단 이력 목록(F-APP-032)에 쓴다. */
    List<DiagnosisEntity> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId, Pageable pageable);
}
