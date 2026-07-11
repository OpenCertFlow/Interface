package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.rule.RuleCode;
import java.util.Set;

/**
 * 표시·라벨링 확인 항목. 사용자가 제품에 표기해야 할 사항이다.
 *
 * @param label         확인할 표시 사항
 * @param matchedRules  이 항목을 만든 룰들
 */
public record LabelingCheckItem(String label, Set<RuleCode> matchedRules) {

    public LabelingCheckItem {
        Guard.hasText(label, "label");
        matchedRules = Set.copyOf(Guard.notEmpty(matchedRules, "matchedRules"));
    }
}
