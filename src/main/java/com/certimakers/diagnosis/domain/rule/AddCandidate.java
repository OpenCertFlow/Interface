package com.certimakers.diagnosis.domain.rule;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.model.SchemeCode;

/** 인증 후보를 추가한다. */
public record AddCandidate(SchemeCode schemeCode, CertificationType type) implements Effect {

    public AddCandidate {
        Guard.notNull(schemeCode, "schemeCode");
        Guard.notNull(type, "type");
    }
}
