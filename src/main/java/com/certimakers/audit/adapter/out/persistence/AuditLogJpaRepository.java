package com.certimakers.audit.adapter.out.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByOrderByOccurredAtDesc(Pageable pageable);

    List<AuditLogEntity> findByActorOrderByOccurredAtDesc(String actor, Pageable pageable);
}
