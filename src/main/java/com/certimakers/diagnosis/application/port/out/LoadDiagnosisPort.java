package com.certimakers.diagnosis.application.port.out;

import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;

/**
 * 아웃바운드 포트: 진단 애그리거트를 ID로 로드한다. 블로킹(JPA).
 *
 * <p>없으면 {@code null}을 반환한다. 서비스가 {@code switchIfEmpty}로 {@code DIAGNOSIS_NOT_FOUND}(404)를 낸다.
 */
public interface LoadDiagnosisPort {

    Diagnosis load(DiagnosisId id);
}
