package com.certimakers.consulting.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultingLeadJpaRepository extends JpaRepository<ConsultingLeadEntity, UUID> {
}
