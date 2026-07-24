package com.certimakers.diagnosis.application.port.out;

/**
 * AI 장애 폴백 스위치(F-WADM-020). 관리자가 RAG 근거 검색·LLM 문장화를 강제로 끌 수 있다.
 *
 * <p>외부 AI가 불안정할 때 관리자가 선제적으로 폴백을 켜, 매 진단이 타임아웃을 기다리지 않고 곧바로
 * 결정론 결과(근거·문장 없이)로 응답하게 한다. 판정 자체는 룰이 하므로 폴백을 켜도 결과의 정확성은
 * 그대로다(ADR-0003) — 근거·설명만 빠진다.
 */
public interface AiFallbackSwitchPort {

    boolean isEvidenceDisabled();

    boolean isNarrationDisabled();

    void setEvidenceDisabled(boolean disabled);

    void setNarrationDisabled(boolean disabled);
}
