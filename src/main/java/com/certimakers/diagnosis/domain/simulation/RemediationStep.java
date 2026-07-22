package com.certimakers.diagnosis.domain.simulation;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.Requirement;

/**
 * 보완 경로의 한 단계. "이 서류를 준비하면 준비도가 몇 %p 올라 몇 %가 된다".
 *
 * @param order                  1부터 시작하는 수행 순서
 * @param documentCode           준비할 서류
 * @param requirement            요구 강도
 * @param weight                 이 서류의 가중치
 * @param scoreAfter             이 단계까지 마쳤을 때의 준비도(%)
 * @param gainPercentagePoints   이 단계로 오르는 폭(%p)
 */
public record RemediationStep(
        int order,
        DocumentCode documentCode,
        Requirement requirement,
        int weight,
        int scoreAfter,
        int gainPercentagePoints) {

    public RemediationStep {
        Guard.positive(order, "order");
        Guard.notNull(documentCode, "documentCode");
        Guard.notNull(requirement, "requirement");
        Guard.positive(weight, "weight");
        Guard.inRange(scoreAfter, 0, 100, "scoreAfter");
    }
}
