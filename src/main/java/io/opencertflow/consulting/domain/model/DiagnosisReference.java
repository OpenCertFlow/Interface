package io.opencertflow.consulting.domain.model;

import io.opencertflow.common.domain.model.Guard;

/**
 * 컨설팅이 참조하는 진단의 식별자. 진단 컨텍스트의 {@code DiagnosisId}를 직접 가져오지 않고 자체
 * 참조 타입을 둔다 — 컨텍스트 간 결합을 낮추기 위함이다. 컨설팅은 "어떤 진단의 Long"만 알면 된다.
 */
public record DiagnosisReference(Long value) {

    public DiagnosisReference {
        Guard.notNull(value, "diagnosisReference");
    }

    public static DiagnosisReference of(Long value) {
        return new DiagnosisReference(value);
    }
}
