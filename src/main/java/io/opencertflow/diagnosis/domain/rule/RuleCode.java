package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;

/**
 * 룰의 안정적 식별자. 예: {@code R-SA-001} (Rule - SAfety - 순번).
 *
 * <p>진단 결과에 매칭된 룰 코드를 함께 저장한다. "왜 이 인증이 후보로 나왔는가"를 사후에 룰까지
 * 역추적하기 위함이며, 이것이 검증 단계의 "규칙 일치 여부 확인" 방법이 된다(03-diagnosis-flow.md).
 */
public record RuleCode(String value) {

    public RuleCode {
        Guard.hasText(value, "ruleCode");
    }

    public static RuleCode of(String value) {
        return new RuleCode(value);
    }
}
