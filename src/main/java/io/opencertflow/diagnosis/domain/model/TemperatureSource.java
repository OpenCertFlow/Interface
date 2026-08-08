package io.opencertflow.diagnosis.domain.model;

/**
 * 최고 표면온도 값의 출처(F-APP-017). 같은 온도라도 근거의 무게가 다르다.
 *
 * <p>{@link #MEASURED}(시험기관 측정)만 인증 판단의 근거로 충분하다. {@link #ESTIMATED}(자체 추정)나
 * {@link #UNKNOWN}(모름)은 근거가 약해 전문가 확인으로 이어진다 — 추정값을 측정값처럼 다루면 화상
 * 위험 판정이 조용히 틀어진다.
 */
public enum TemperatureSource {

    MEASURED("시험기관 측정"),
    ESTIMATED("자체 추정"),
    UNKNOWN("모름");

    private final String displayName;

    TemperatureSource(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
