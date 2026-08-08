package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;

/**
 * 룰셋의 버전. 진단 결과에 평가 시점의 버전을 스냅샷으로 함께 저장하여 재현성을 확보한다.
 *
 * <p>룰셋이 나중에 삭제·변경되어도 "이 진단은 v3 룰로 평가되었다"는 사실이 남아야 한다.
 * 감사 추적을 외래키에 의존하면 안 된다(05-data-model.md).
 */
public record RuleSetVersion(int value) {

    public RuleSetVersion {
        Guard.positive(value, "ruleSetVersion");
    }

    public static RuleSetVersion of(int value) {
        return new RuleSetVersion(value);
    }
}
