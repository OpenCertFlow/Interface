package io.opencertflow.consulting.adapter.out.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultingLeadJpaRepository extends JpaRepository<ConsultingLeadEntity, Long> {

    List<ConsultingLeadEntity> findByOrderByCreatedAtDesc(Pageable pageable);

    List<ConsultingLeadEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<ConsultingLeadEntity> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId, Pageable pageable);

    /** 보존 기간 파기(F-BE-014). 삭제 건수를 돌려준다. 자식(메시지·동의)은 FK CASCADE로 함께 지워진다. */
    long deleteByStatusInAndCreatedAtBefore(Collection<String> statuses, Instant threshold);
}
