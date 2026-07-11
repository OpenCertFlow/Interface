package com.certimakers.diagnosis.domain.model;

/**
 * KC 인증 유형. "내 제품이 어디에 해당하는지"의 세 갈래(기획서).
 *
 * <p>이것은 <b>후보</b> 유형이지 확정 판정이 아니다. Rule Engine이 식별하고 RAG가 근거를 붙이며,
 * 최종 판정은 전문가 상담의 몫이다(ADR-0003).
 */
public enum CertificationType {

    /** 안전인증 — 위해도가 높아 모델별 제품시험과 공장심사를 요구하는 유형 */
    SAFETY_CERT("안전인증"),

    /** 안전확인 — 지정 시험기관의 제품시험으로 안전성을 확인하는 유형 */
    SAFETY_CONFIRM("안전확인"),

    /** 공급자적합성확인 — 제조·수입자가 스스로 시험하거나 시험을 의뢰해 확인하는 유형 */
    SUPPLIER_DOC("공급자적합성확인");

    private final String displayName;

    CertificationType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
