package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.UUID;

/**
 * 진단 애그리거트의 식별자. 값은 UUIDv7(시간 정렬)이며 {@code IdGenerator} 포트가 생성한다.
 */
public record DiagnosisId(UUID value) {

    public DiagnosisId {
        Guard.notNull(value, "diagnosisId");
    }

    public static DiagnosisId of(UUID value) {
        return new DiagnosisId(value);
    }
}
