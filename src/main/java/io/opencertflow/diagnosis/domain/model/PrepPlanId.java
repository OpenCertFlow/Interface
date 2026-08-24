package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.model.Guard;

/** 준비계획 식별자. 값은 전역 시퀀스에서 나오며 {@code IdGenerator} 포트가 생성한다. */
public record PrepPlanId(Long value) {

    public PrepPlanId {
        Guard.notNull(value, "prepPlanId");
    }

    public static PrepPlanId of(Long value) {
        return new PrepPlanId(value);
    }
}
