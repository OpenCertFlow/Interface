package com.certimakers.diagnosis.domain.rule;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import java.util.List;

/**
 * 하나의 룰. 조건이 참이면 효과들을 낸다.
 *
 * @param code       룰 식별자 (예: R-SA-001)
 * @param priority   우선순위. 높을수록 나중에 적용되어 결정적 순서를 만든다
 * @param condition  매칭 조건
 * @param effects    매칭 시 낼 효과들
 */
public record Rule(RuleCode code, int priority, Condition condition, List<Effect> effects) {

    public Rule {
        Guard.notNull(code, "code");
        Guard.notNull(condition, "condition");
        effects = List.copyOf(Guard.notEmpty(effects, "effects"));
    }

    public boolean matches(ProductProfile profile) {
        return condition.test(profile);
    }
}
