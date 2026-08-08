package io.opencertflow.diagnosis.domain.simulation;

import io.opencertflow.common.domain.model.Guard;
import java.util.List;

/**
 * 목표 준비도에 도달하기 위한 최소 보완 경로.
 *
 * <p>기존 "보완 우선순위"는 누락 서류를 가중치 내림차순으로 나열한 것이다(03-diagnosis-flow.md).
 * 그것은 <b>무엇부터</b>는 알려주지만 <b>어디까지</b>는 알려주지 못한다. 이 계획은 목표를 받아
 * "몇 개만 준비하면 되는지"를 끊어 준다 — 소공인에게는 전체 목록보다 훨씬 실행 가능한 정보다.
 *
 * @param currentScore     현재 준비도(%)
 * @param targetScore      사용자가 요청한 목표 준비도(%)
 * @param applicable       점수 산정이 가능한 진단인지. false면 나머지 필드는 의미 없다(불변식 2)
 * @param achievable       요구 서류를 모두 갖춰도 목표에 도달할 수 있는지
 * @param projectedScore   계획을 모두 수행했을 때의 준비도(%)
 * @param steps            수행 순서대로 정렬된 보완 단계
 * @param remainingMissing 계획 수행 후에도 남는 누락 서류 수
 */
public record RemediationPlan(
        int currentScore,
        int targetScore,
        boolean applicable,
        boolean achievable,
        int projectedScore,
        List<RemediationStep> steps,
        int remainingMissing) {

    public RemediationPlan {
        steps = List.copyOf(Guard.notNull(steps, "steps"));
    }

    /** 점수를 낼 수 없는 진단(요구 서류 자체가 없음)에 대한 계획. */
    public static RemediationPlan notApplicable(int targetScore) {
        return new RemediationPlan(0, targetScore, false, false, 0, List.of(), 0);
    }

    /** 이미 목표를 넘긴 상태. 준비할 것이 없다. */
    public static RemediationPlan alreadyMet(int currentScore, int targetScore) {
        return new RemediationPlan(currentScore, targetScore, true, true, currentScore, List.of(), 0);
    }

    public int documentCount() {
        return steps.size();
    }
}
