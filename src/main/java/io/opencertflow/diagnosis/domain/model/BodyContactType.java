package io.opencertflow.diagnosis.domain.model;

/**
 * 발열 제품이 신체에 닿는 방식(F-APP-014). 화상 위험 판단의 핵심 입력이며, 접촉 강도 순으로 나열한다.
 *
 * <p>확실히 닿는 세 값({@link #DIRECT_SKIN}·{@link #THROUGH_CLOTHING}·{@link #THROUGH_COVER})만
 * "신체에 닿는" 제품이다({@link #contactsBody()}). 룰은 이 구분으로 화상 위험 기준을 나눈다.
 * {@link #UNKNOWN}(모름)은 없음/비접촉과 구분해 임의 판정하지 않고 전문가 확인으로 보낸다.
 */
public enum BodyContactType {

    DIRECT_SKIN("직접 피부 접촉"),
    THROUGH_CLOTHING("의류 위 접촉"),
    THROUGH_COVER("전용 커버를 통한 접촉"),
    NONE("비접촉"),
    UNKNOWN("모름");

    private final String displayName;

    BodyContactType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean contactsBody() {
        return this == DIRECT_SKIN || this == THROUGH_CLOTHING || this == THROUGH_COVER;
    }
}
