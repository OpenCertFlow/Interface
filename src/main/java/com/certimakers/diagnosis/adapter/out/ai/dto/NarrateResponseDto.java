package com.certimakers.diagnosis.adapter.out.ai.dto;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.domain.model.Narration;
import java.util.List;

/**
 * AI 워커 {@code POST /narrate} 응답 본문. LLM이 생성한 문장이다.
 *
 * <p>{@code templateFallback = false}로 도메인 Narration을 만든다 — LLM이 실제로 응답한 경우이므로.
 * 폴백은 서비스가 이 어댑터의 실패를 감지했을 때 {@code TemplateNarrator}로 처리한다.
 */
public record NarrateResponseDto(
        String summary,
        List<String> nextActions,
        List<String> preConsultQuestions,
        String disclaimer,
        String modelId) {

    public Narration toDomain() {
        if (summary == null || summary.isBlank()) {
            throw BusinessException.invalid("AI 워커가 빈 요약을 반환했습니다.");
        }
        return new Narration(
                summary,
                nextActions == null ? List.of() : nextActions,
                preConsultQuestions == null ? List.of() : preConsultQuestions,
                disclaimer == null || disclaimer.isBlank()
                        ? "본 결과는 사전 점검 지표이며 인증 합격을 보장하지 않습니다."
                        : disclaimer,
                modelId == null || modelId.isBlank() ? "unknown" : modelId,
                false);
    }
}
