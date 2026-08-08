package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import java.util.List;

/** 논리합. 하나라도 참이면 참. */
public record AnyOf(List<Condition> conditions) implements Condition {

    public AnyOf {
        conditions = List.copyOf(Guard.notEmpty(conditions, "conditions"));
    }

    public static AnyOf of(Condition... conditions) {
        return new AnyOf(List.of(conditions));
    }

    @Override
    public boolean test(ProductProfile profile) {
        return conditions.stream().anyMatch(condition -> condition.test(profile));
    }
}
