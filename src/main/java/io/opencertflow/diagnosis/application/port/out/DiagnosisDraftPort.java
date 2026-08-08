package io.opencertflow.diagnosis.application.port.out;

import io.opencertflow.diagnosis.domain.model.DiagnosisDraft;
import java.util.List;
import java.util.Optional;

/** 진단 초안(F-APP-004) 아웃바운드 포트. 블로킹(JPA). 소유권 검사는 서비스가 한다. */
public interface DiagnosisDraftPort {

    /** 새 초안을 저장하거나 기존 초안을 갱신한다. */
    DiagnosisDraft save(DiagnosisDraft draft);

    Optional<DiagnosisDraft> findById(long id);

    /** 소유자의 초안을 최신 수정순으로. */
    List<DiagnosisDraft> findByOwner(String ownerUserId);

    void deleteById(long id);
}
