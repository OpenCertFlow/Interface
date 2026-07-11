package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.List;

/**
 * 리포트 설명 문장. LLM이 규칙 결과와 근거를 사용자가 이해하기 쉬운 문장으로 옮긴 것이다.
 *
 * <p>LLM 호출이 실패하거나 지연되면 템플릿 문장으로 대체하며, 그 경우 {@code templateFallback}이
 * true다. 이 값이 곧 진단을 COMPLETED_DEGRADED로 만드는 신호가 된다(03-diagnosis-flow.md).
 *
 * @param summary             핵심 요약
 * @param nextActions         쉬운 다음 행동 목록
 * @param preConsultQuestions 상담 전 질문 목록
 * @param disclaimer          면책 문구
 * @param modelId             생성에 쓴 LLM 식별자 (템플릿이면 "template")
 * @param templateFallback    LLM 대신 템플릿으로 생성됐는지 여부
 */
public record Narration(
        String summary,
        List<String> nextActions,
        List<String> preConsultQuestions,
        String disclaimer,
        String modelId,
        boolean templateFallback) {

    public Narration {
        Guard.hasText(summary, "summary");
        nextActions = List.copyOf(Guard.notNull(nextActions, "nextActions"));
        preConsultQuestions = List.copyOf(Guard.notNull(preConsultQuestions, "preConsultQuestions"));
        Guard.hasText(disclaimer, "disclaimer");
        Guard.hasText(modelId, "modelId");
    }

    public boolean isTemplateFallback() {
        return templateFallback;
    }
}
