package com.certimakers.diagnosis.domain.rule;

/**
 * 속성 값의 종류. 룰을 JSON으로 저장·로드할 때 값을 올바른 타입으로 되돌리기 위한 정보다.
 *
 * <p>이 enum이 도메인에 있는 이유는, 값 타입 지식이 도메인 개념이기 때문이다. 반면 JSON 파싱 자체는
 * 영속성 어댑터의 코덱이 담당한다 — 도메인은 Jackson을 참조하지 않는다(ArchUnit).
 */
public enum ValueKind {
    BOOLEAN,
    INTEGER,
    TARGET_USER,
    SALES_CHANNEL,
    PRODUCT_GROUP,
    MATERIAL,
    DOCUMENT_CODE,
    BODY_CONTACT_TYPE,
    CONTROLLER_STATUS,
    TEMPERATURE_SOURCE
}
