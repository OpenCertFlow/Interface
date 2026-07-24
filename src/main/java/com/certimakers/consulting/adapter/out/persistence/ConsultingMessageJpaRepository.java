package com.certimakers.consulting.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultingMessageJpaRepository extends JpaRepository<ConsultingMessageEntity, UUID> {

    List<ConsultingMessageEntity> findByLeadIdOrderByCreatedAtAsc(UUID leadId);
}
