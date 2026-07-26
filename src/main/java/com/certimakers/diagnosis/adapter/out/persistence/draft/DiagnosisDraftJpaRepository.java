package com.certimakers.diagnosis.adapter.out.persistence.draft;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisDraftJpaRepository extends JpaRepository<DiagnosisDraftEntity, Long> {

    /** 소유자의 초안을 최신 수정순으로(F-APP-004 목록). */
    List<DiagnosisDraftEntity> findByOwnerUserIdOrderByUpdatedAtDesc(String ownerUserId);
}
