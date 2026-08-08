package io.opencertflow.diagnosis.domain.model;

/**
 * 온도 조절 방식(결정문 §5.2). 온도조절기가 있을 때 어떤 방식으로 조절하는지를 나눈다.
 *
 * <p>{@link #STEP}만 "조절 단계 수"가 의미를 갖는다. {@link #CONTINUOUS}(연속 조절)나
 * {@link #OTHER}(기타·미확인)는 단계 수가 없다. 미확인은 임의 판정하지 않고 전문가 확인으로 보낸다.
 */
public enum AdjustmentMode {

    STEP("단계 조절"),
    CONTINUOUS("연속 조절"),
    OTHER("기타·미확인");

    private final String displayName;

    AdjustmentMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
