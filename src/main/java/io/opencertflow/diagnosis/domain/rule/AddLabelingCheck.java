package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;

/** 표시·라벨링 확인 항목을 추가한다. */
public record AddLabelingCheck(String label) implements Effect {

    public AddLabelingCheck {
        Guard.hasText(label, "label");
    }
}
