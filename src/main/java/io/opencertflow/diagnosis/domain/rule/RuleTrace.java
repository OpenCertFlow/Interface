package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;
import java.util.List;

/**
 * 룰 하나가 발동한 기록. "무엇 때문에 켜졌고, 그래서 무엇을 냈는가"를 한 덩어리로 담는다.
 *
 * <p>진단 결과에 함께 저장한다. 룰셋은 개정되므로 나중에 다시 평가해 복원하려 하면 그때의 룰이
 * 남아 있어야 하는데, 그 보장을 전제로 두는 대신 결과 자체를 스냅샷으로 남긴다 — 이미 룰셋 버전과
 * 가중치를 그렇게 다루고 있다(05-data-model.md).
 *
 * @param ruleCode 발동한 룰
 * @param priority 룰 우선순위. 같은 서류를 여러 룰이 요구할 때 순서를 설명한다
 * @param facts    매칭에 기여한 단말 조건들
 * @param effects  그 결과로 낸 효과를 사람이 읽을 수 있게 옮긴 문장
 */
public record RuleTrace(
        String ruleCode,
        int priority,
        List<ConditionFact> facts,
        List<String> effects) {

    public RuleTrace {
        Guard.hasText(ruleCode, "ruleCode");
        facts = List.copyOf(Guard.notNull(facts, "facts"));
        effects = List.copyOf(Guard.notNull(effects, "effects"));
    }
}
