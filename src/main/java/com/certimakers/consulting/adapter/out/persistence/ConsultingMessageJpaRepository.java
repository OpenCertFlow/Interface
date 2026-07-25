package com.certimakers.consulting.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultingMessageJpaRepository extends JpaRepository<ConsultingMessageEntity, Long> {

    List<ConsultingMessageEntity> findByLeadIdOrderByCreatedAtAsc(Long leadId);
}
