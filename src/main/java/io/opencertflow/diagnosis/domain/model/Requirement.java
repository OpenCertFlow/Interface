package io.opencertflow.diagnosis.domain.model;

/**
 * 서류의 요구 강도. 준비도 점수 가중치의 근거가 된다.
 *
 * <p>여기 담긴 기본 가중치는 {@code ScoreRubric}이 재정의할 수 있다. 가중치 기준표는 산출물이며
 * 심사에서 근거를 요구받으므로, 코드가 아니라 데이터로 관리하는 것이 원칙이다. 이 값은 어디까지나
 * 기준표가 없을 때의 폴백이다.
 */
public enum Requirement {

    /** 필수 서류. 없으면 인증 진행 자체가 막힌다. 더 높은 가중치. */
    REQUIRED("필수", 3),

    /** 권장 서류. 있으면 상담이 수월해진다. */
    RECOMMENDED("권장", 1);

    private final String displayName;
    private final int defaultWeight;

    Requirement(String displayName, int defaultWeight) {
        this.displayName = displayName;
        this.defaultWeight = defaultWeight;
    }

    public String displayName() {
        return displayName;
    }

    public int defaultWeight() {
        return defaultWeight;
    }

    /** 두 요구 강도 중 더 강한 것. 여러 룰이 같은 서류를 요구하면 REQUIRED가 이긴다. */
    public Requirement strongerOf(Requirement other) {
        return this == REQUIRED || other == REQUIRED ? REQUIRED : RECOMMENDED;
    }
}
