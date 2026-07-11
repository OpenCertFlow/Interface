package com.certimakers.diagnosis.domain.rule;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.ProductProfile;

/** 논리 부정. "어린이용이 아닌 경우" 같은 조건에 쓰인다. */
public record Not(Condition condition) implements Condition {

    public Not {
        Guard.notNull(condition, "condition");
    }

    public static Not of(Condition condition) {
        return new Not(condition);
    }

    @Override
    public boolean test(ProductProfile profile) {
        return !condition.test(profile);
    }
}
