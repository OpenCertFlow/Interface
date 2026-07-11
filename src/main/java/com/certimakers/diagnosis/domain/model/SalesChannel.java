package com.certimakers.diagnosis.domain.model;

/** 판매 방식. 온라인 판매는 표시·라벨링 확인 항목에 영향을 준다. */
public enum SalesChannel {

    ONLINE("온라인"),
    OFFLINE("오프라인"),
    BOTH("온·오프라인");

    private final String displayName;

    SalesChannel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
