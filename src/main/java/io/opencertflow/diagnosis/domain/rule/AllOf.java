package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import java.util.List;

/** 논리곱. 모든 하위 조건이 참일 때 참. 빈 조건은 참(항등원)이 아니라 오류로 막는다. */
public record AllOf(List<Condition> conditions) implements Condition {

    public AllOf {
        conditions = List.copyOf(Guard.notEmpty(conditions, "conditions"));
    }

    public static AllOf of(Condition... conditions) {
        return new AllOf(List.of(conditions));
    }

    @Override
    public boolean test(ProductProfile profile) {
        return conditions.stream().allMatch(condition -> condition.test(profile));
    }
}
