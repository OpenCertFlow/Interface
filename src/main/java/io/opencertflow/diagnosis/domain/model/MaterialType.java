package io.opencertflow.diagnosis.domain.model;

/** 주요 재질. 재질에 따라 유해물질 관련 확인 항목이 달라질 수 있다. */
public enum MaterialType {

    PLASTIC("플라스틱"),
    METAL("금속"),
    RUBBER("고무"),
    GLASS("유리"),
    TEXTILE("섬유"),
    OTHER("기타");

    private final String displayName;

    MaterialType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
