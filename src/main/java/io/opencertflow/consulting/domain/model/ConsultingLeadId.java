package io.opencertflow.consulting.domain.model;

import io.opencertflow.common.domain.model.Guard;

/** 컨설팅 리드 식별자. 값은 전역 시퀀스에서 나온다. */
public record ConsultingLeadId(Long value) {

    public ConsultingLeadId {
        Guard.notNull(value, "consultingLeadId");
    }

    public static ConsultingLeadId of(Long value) {
        return new ConsultingLeadId(value);
    }
}
