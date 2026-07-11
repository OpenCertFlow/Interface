package com.certimakers.diagnosis.domain.model;

/**
 * 사용 대상. 어린이제품 여부는 인증 유형을 크게 가르므로 별도 값으로 둔다.
 */
public enum TargetUser {

    GENERAL("일반"),
    CHILD("어린이"),
    INDUSTRIAL("산업용");

    private final String displayName;

    TargetUser(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
