package com.certimakers.diagnosis.domain.model;

/**
 * 온도조절기 유무 상태(F-APP-016). "모름"을 별도 상태로 둔 이유는, 소공인이 조절기 탑재 여부를
 * 확실히 모르는 경우가 흔하기 때문이다.
 *
 * <p>{@link #UNKNOWN}은 {@link #ABSENT}(없음)와 다르다. 없음은 과열 위험 경고로 이어지지만, 모름은
 * 판단 불가이므로 전문가 확인(AMBIGUOUS_CONDITION)으로 보낸다 — 모른다를 없음으로 뭉개면 잘못된
 * 경고가 뜬다.
 */
public enum ControllerStatus {

    PRESENT("있음"),
    ABSENT("없음"),
    UNKNOWN("모름");

    private final String displayName;

    ControllerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
