package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.rule.RuleCode;
import java.util.Set;

/**
 * 룰이 식별한 인증 검토 후보. 확정 판정이 아니다.
 *
 * <p>{@code matchedRules}는 "왜 이 후보가 나왔는가"의 답이다. 심사위원의 질문에 즉시 답하고,
 * 검증 단계에서 규칙 일치 여부를 확인하는 근거가 된다.
 *
 * @param schemeCode    인증 제도 코드
 * @param type          인증 유형
 * @param matchedRules  이 후보를 만든 룰들
 */
public record CertificationCandidate(
        SchemeCode schemeCode,
        CertificationType type,
        Set<RuleCode> matchedRules) {

    public CertificationCandidate {
        Guard.notNull(schemeCode, "schemeCode");
        Guard.notNull(type, "type");
        matchedRules = Set.copyOf(Guard.notEmpty(matchedRules, "matchedRules"));
    }
}
