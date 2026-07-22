package com.certimakers.diagnosis.adapter.in.web;

import java.util.List;

/**
 * 최소 보완 경로 응답. "무엇부터"가 아니라 <b>"어디까지 하면 되는지"</b>를 알려준다.
 *
 * @param diagnosisId      기준이 된 진단 ID
 * @param applicable       점수 산정이 가능한 진단인지. false면 나머지 값은 의미 없다
 * @param currentScore     현재 준비도(%)
 * @param targetScore      요청한 목표 준비도(%)
 * @param achievable       목표 도달이 가능한지
 * @param projectedScore   계획을 모두 수행했을 때의 준비도(%)
 * @param documentCount    준비해야 하는 서류 수
 * @param steps            수행 순서대로의 보완 단계
 * @param remainingMissing 계획 수행 후에도 남는 누락 서류 수
 * @param notice           준비도의 의미를 알리는 고지 문구
 */
public record RemediationPlanResponse(
        String diagnosisId,
        boolean applicable,
        int currentScore,
        int targetScore,
        boolean achievable,
        int projectedScore,
        int documentCount,
        List<StepView> steps,
        int remainingMissing,
        String notice) {

    /**
     * @param order                수행 순서 (1부터)
     * @param documentCode         준비할 서류
     * @param requirement          요구 강도 (REQUIRED·RECOMMENDED)
     * @param weight               가중치
     * @param scoreAfter           이 단계까지 마쳤을 때의 준비도(%)
     * @param gainPercentagePoints 이 단계로 오르는 폭(%p)
     */
    public record StepView(
            int order,
            String documentCode,
            String requirement,
            int weight,
            int scoreAfter,
            int gainPercentagePoints) {
    }
}
