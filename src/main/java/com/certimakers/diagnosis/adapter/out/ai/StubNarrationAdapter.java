package com.certimakers.diagnosis.adapter.out.ai;

import com.certimakers.common.adapter.out.external.annotation.ExternalAdapter;
import com.certimakers.diagnosis.application.port.out.NarrateReportPort;
import com.certimakers.diagnosis.application.port.out.NarrationRequest;
import com.certimakers.diagnosis.domain.model.Narration;
import com.certimakers.diagnosis.domain.service.TemplateNarrator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

/**
 * AI 워커가 없을 때 쓰는 문장화 스텁. {@code local} 프로파일에서만 활성화된다.
 *
 * <p>규칙 결과를 그대로 담은 최소 문장을 돌려준다. 여기서 반환하는 Narration은 {@code
 * templateFallback = false}이므로, LLM이 정상 응답한 상황을 흉내 낸다 — 즉 로컬 데모에서는
 * 진단이 COMPLETED로 끝난다. 실제 폴백 문장 조립은 도메인의 {@link TemplateNarrator}가 담당하며,
 * 그것은 서비스가 이 포트의 실패를 감지했을 때 쓰인다.
 */
@ExternalAdapter
@Profile("local")
public class StubNarrationAdapter implements NarrateReportPort {

    private static final String MODEL_ID = "stub";
    private static final String DISCLAIMER =
            "본 결과는 인증 준비 상태를 확인하기 위한 사전 점검 지표이며, 인증 합격을 보장하지 않습니다.";

    @Override
    public Mono<Narration> narrate(NarrationRequest request) {
        String summary = request.candidates().isEmpty()
                ? "적용 가능한 인증 규칙을 찾지 못했습니다. 전문가 확인이 필요합니다. (스텁)"
                : "입력하신 제품은 인증 검토 대상으로 보입니다. 아래 서류와 표시 사항을 확인하세요. (스텁)";
        return Mono.just(new Narration(
                summary,
                List.of("누락된 서류를 준비하세요.", "인증 전문가 상담으로 다음 단계를 확인하세요."),
                List.of("정격전압과 소비전력을 정확히 확인해 주세요."),
                DISCLAIMER,
                MODEL_ID,
                false));
    }
}
