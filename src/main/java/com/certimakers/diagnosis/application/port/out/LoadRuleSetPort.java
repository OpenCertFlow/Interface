package com.certimakers.diagnosis.application.port.out;

import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.rule.RuleSet;

/**
 * 아웃바운드 포트: 제품군의 활성 룰셋을 로드한다. 블로킹(JPA)이므로 호출자는 {@code BlockingBridge}로 감싼다.
 *
 * <p>활성 룰셋이 없으면 {@code null}을 반환한다. {@code BlockingBridge.mono}가 이를 빈 {@code Mono}로
 * 바꾸고, 서비스는 {@code switchIfEmpty}로 {@code RULE_SET_NOT_FOUND}(503)를 낸다. 이것이 폴백 없는
 * 진단 실패 지점이다 — 룰 없이는 판정할 수 없다.
 */
public interface LoadRuleSetPort {

    RuleSet loadActive(ProductGroup productGroup);
}
