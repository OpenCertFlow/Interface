package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;

/**
 * 진단 애그리거트의 식별자. 값은 전역 시퀀스에서 나오며 {@code IdGenerator} 포트가 생성한다.
 */
public record DiagnosisId(Long value) {

    public DiagnosisId {
        Guard.notNull(value, "diagnosisId");
    }

    public static DiagnosisId of(Long value) {
        return new DiagnosisId(value);
    }
}
