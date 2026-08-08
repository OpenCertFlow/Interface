package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.model.Guard;

/**
 * 준비도 점수. "공식 요구자료 대비 현재 준비 수준"이며, 인증 합격·불합격 판정이 아니다(기획서).
 *
 * <p>불변식 2(04-domain-model.md): 요구 서류의 총 가중치가 0이면 점수는 0이 아니라 <b>산정 불가</b>다.
 * 요구 서류가 없다는 것은 룰이 아무것도 잡지 못했다는 뜻이고, 그 경우 0%는 "준비가 하나도 안 됨"이라는
 * 잘못된 메시지를 준다. {@link #applicable}이 이 둘을 구분한다.
 *
 * @param applicable    산정 가능 여부. false면 percentage는 의미 없다
 * @param percentage    준비도(%). applicable일 때 0~100
 * @param earnedWeight  보유한 요구 서류의 가중치 합
 * @param totalWeight   전체 요구 서류의 가중치 합
 */
public record ReadinessScore(
        boolean applicable,
        int percentage,
        int earnedWeight,
        int totalWeight) {

    public ReadinessScore {
        if (applicable) {
            Guard.inRange(percentage, 0, 100, "percentage");
            Guard.isTrue(totalWeight > 0, "산정 가능한 점수의 총 가중치는 0보다 커야 합니다.");
            Guard.isTrue(earnedWeight >= 0 && earnedWeight <= totalWeight,
                    "획득 가중치는 0 이상 총 가중치 이하여야 합니다.");
        }
    }

    /** 요구 서류가 없어 점수를 낼 수 없는 상태. 리포트에는 "전문가 확인 필요"로 이어진다. */
    public static ReadinessScore notApplicable() {
        return new ReadinessScore(false, 0, 0, 0);
    }

    public static ReadinessScore of(int earnedWeight, int totalWeight) {
        if (totalWeight <= 0) {
            return notApplicable();
        }
        int percentage = Math.round((float) earnedWeight / totalWeight * 100);
        return new ReadinessScore(true, percentage, earnedWeight, totalWeight);
    }
}
