package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.model.ProductGroup;
import java.util.Comparator;
import java.util.List;

/**
 * 특정 시점에 활성화된, 한 제품군의 룰 집합. 버전을 가진다.
 *
 * <p>{@link #inPriorityOrder()}는 우선순위 오름차순(같으면 룰 코드순)으로 정렬된 룰을 준다.
 * 평가는 이 순서로 이루어져, 같은 입력이 항상 같은 순서의 결과를 낸다(재현성).
 */
public record RuleSet(RuleSetVersion version, ProductGroup productGroup, List<Rule> rules) {

    public RuleSet {
        Guard.notNull(version, "version");
        Guard.notNull(productGroup, "productGroup");
        rules = List.copyOf(Guard.notEmpty(rules, "rules"));
    }

    /** 우선순위 오름차순, 동률이면 룰 코드 사전순. 결정적 평가 순서를 보장한다. */
    public List<Rule> inPriorityOrder() {
        return rules.stream()
                .sorted(Comparator.comparingInt(Rule::priority)
                        .thenComparing(rule -> rule.code().value()))
                .toList();
    }
}
