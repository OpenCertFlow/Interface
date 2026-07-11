package com.certimakers.diagnosis.application.port.out;

import com.certimakers.diagnosis.domain.model.Narration;
import reactor.core.publisher.Mono;

/**
 * 아웃바운드 포트: LLM으로 리포트 문장을 생성한다. 논블로킹(WebClient).
 *
 * <p>실패·타임아웃은 폴백 대상이다. 서비스가 {@code TemplateNarrator}의 템플릿 문장으로 대체하며,
 * 이 경우 진단은 COMPLETED_DEGRADED가 된다. LLM 실패는 진단 실패가 아니다(ADR-0003).
 */
public interface NarrateReportPort {

    Mono<Narration> narrate(NarrationRequest request);
}
