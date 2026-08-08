package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.Requirement;

/** 서류를 요구한다. 같은 서류를 여러 룰이 요구하면 더 강한 요구 강도가 이긴다. */
public record RequireDocument(DocumentCode documentCode, Requirement requirement) implements Effect {

    public RequireDocument {
        Guard.notNull(documentCode, "documentCode");
        Guard.notNull(requirement, "requirement");
    }
}
