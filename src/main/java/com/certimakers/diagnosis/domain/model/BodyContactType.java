package com.certimakers.diagnosis.domain.model;

/**
 * 발열 제품이 신체에 닿는 방식(F-APP-014). 화상 위험 판단의 핵심 입력이며, 접촉 강도 순으로 나열한다.
 *
 * <p>{@link #NONE}을 제외하면 모두 "신체에 닿는" 제품이다({@link #contactsBody()}). 룰은 이 구분으로
 * 화상 위험 기준을 나눈다 — 직접 피부 접촉과 커버를 통한 접촉은 요구되는 확인이 다를 수 있다.
 */
public enum BodyContactType {

    DIRECT_SKIN("직접 피부 접촉"),
    THROUGH_CLOTHING("의류 위 접촉"),
    THROUGH_COVER("전용 커버를 통한 접촉"),
    NONE("비접촉");

    private final String displayName;

    BodyContactType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean contactsBody() {
        return this != NONE;
    }
}
