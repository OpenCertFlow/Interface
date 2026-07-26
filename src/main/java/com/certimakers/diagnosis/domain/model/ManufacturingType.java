package com.certimakers.diagnosis.domain.model;

/**
 * 제조 형태(F-APP-006). 인증 준비에서 책임 주체(제조자·수입자)와 필요한 서류가 달라진다 —
 * 수입품은 제조자 자료를 확보하기 어렵고, OEM/ODM은 설계·제조 책임의 경계 확인이 필요하다.
 *
 * <p>{@link #UNKNOWN}(모름)은 임의 판정하지 않고 전문가 확인으로 보낸다.
 */
public enum ManufacturingType {

    SELF_MADE("자체 제조"),
    IMPORTED("수입"),
    OEM("OEM(위탁 제조)"),
    ODM("ODM(제조자 개발·생산)"),
    UNKNOWN("모름");

    private final String displayName;

    ManufacturingType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
