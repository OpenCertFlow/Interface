package com.certimakers.consulting.adapter.out.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultingLeadJpaRepository extends JpaRepository<ConsultingLeadEntity, Long> {

    List<ConsultingLeadEntity> findByOrderByCreatedAtDesc(Pageable pageable);

    List<ConsultingLeadEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<ConsultingLeadEntity> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId, Pageable pageable);
}
