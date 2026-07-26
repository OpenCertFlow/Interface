package com.certimakers.diagnosis.application.port.out;

import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.model.DiagnosisSummary;
import java.util.List;

/** 진단 이력(F-APP-032/035) 아웃바운드 포트. 블로킹(JPA). */
public interface DiagnosisHistoryPort {

    /** 소유자의 진단을 최신순으로 최대 {@code limit}개 요약해 돌려준다. */
    List<DiagnosisSummary> findByOwner(String ownerUserId, int limit);

    /** 진단을 삭제한다. 자식(프로필·후보·근거 등)은 cascade로 함께 지워진다. 소유권 검사는 서비스가 한다. */
    void deleteById(DiagnosisId id);
}
