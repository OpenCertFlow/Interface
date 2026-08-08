package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.model.CertificationType;
import io.opencertflow.diagnosis.domain.model.SchemeCode;

/** 인증 후보를 추가한다. */
public record AddCandidate(SchemeCode schemeCode, CertificationType type) implements Effect {

    public AddCandidate {
        Guard.notNull(schemeCode, "schemeCode");
        Guard.notNull(type, "type");
    }
}
