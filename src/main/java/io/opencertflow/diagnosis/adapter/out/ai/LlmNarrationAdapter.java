package io.opencertflow.diagnosis.adapter.out.ai;

import io.opencertflow.common.adapter.out.external.annotation.ExternalAdapter;
import io.opencertflow.common.domain.error.ExternalSystemException;
import io.opencertflow.diagnosis.adapter.out.ai.dto.NarrateRequestDto;
import io.opencertflow.diagnosis.adapter.out.ai.dto.NarrateResponseDto;
import io.opencertflow.diagnosis.application.port.out.NarrateReportPort;
import io.opencertflow.diagnosis.application.port.out.NarrationRequest;
import io.opencertflow.diagnosis.domain.model.Narration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * {@link NarrateReportPort}의 실제 구현. AI 워커의 {@code POST /narrate}를 호출한다.
 *
 * <p>{@code !local} 프로파일에서만 활성화된다. 실패·오류는 {@link ExternalSystemException}으로
 * 알리고, 템플릿 폴백은 서비스가 결정한다(ADR-0003).
 */
@ExternalAdapter
@Profile("!local")
public class LlmNarrationAdapter implements NarrateReportPort {

    private static final String SYSTEM = "ai-worker/narrate";

    private final WebClient aiWorkerWebClient;

    public LlmNarrationAdapter(WebClient aiWorkerWebClient) {
        this.aiWorkerWebClient = aiWorkerWebClient;
    }

    @Override
    public Mono<Narration> narrate(NarrationRequest request) {
        return aiWorkerWebClient.post()
                .uri("/narrate")
                .bodyValue(NarrateRequestDto.from(request))
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> ExternalSystemException.of(
                                SYSTEM, "문장화 응답 오류: %d".formatted(response.statusCode().value()), null)))
                .bodyToMono(NarrateResponseDto.class)
                .map(NarrateResponseDto::toDomain);
    }
}
