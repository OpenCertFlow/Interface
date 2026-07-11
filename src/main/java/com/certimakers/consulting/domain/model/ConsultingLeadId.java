package com.certimakers.consulting.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.UUID;

/** 컨설팅 리드 식별자. UUIDv7. */
public record ConsultingLeadId(UUID value) {

    public ConsultingLeadId {
        Guard.notNull(value, "consultingLeadId");
    }

    public static ConsultingLeadId of(UUID value) {
        return new ConsultingLeadId(value);
    }
}
