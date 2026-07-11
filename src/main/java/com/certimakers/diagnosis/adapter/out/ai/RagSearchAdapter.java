package com.certimakers.diagnosis.adapter.out.ai;

import com.certimakers.common.adapter.out.external.annotation.ExternalAdapter;
import com.certimakers.common.domain.error.ExternalSystemException;
import com.certimakers.diagnosis.adapter.out.ai.dto.SearchRequestDto;
import com.certimakers.diagnosis.adapter.out.ai.dto.SearchResponseDto;
import com.certimakers.diagnosis.application.port.out.EvidenceQuery;
import com.certimakers.diagnosis.application.port.out.SearchEvidencePort;
import com.certimakers.diagnosis.domain.model.Evidence;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * {@link SearchEvidencePort}의 실제 구현. AI 워커의 {@code POST /search}를 호출한다.
 *
 * <p>{@code !local} 프로파일에서만 활성화된다 — 로컬 데모에서는 스텁이, 실제 배포에서는 이 어댑터가
 * 쓰인다. 응답 시간 제한(타임아웃)과 폴백 결정은 어댑터가 아니라 서비스의 정책이다(ADR-0004).
 * 어댑터는 실패를 {@link ExternalSystemException}으로 정직하게 알릴 뿐, 빈 결과를 지어내지 않는다.
 */
@ExternalAdapter
@Profile("!local")
public class RagSearchAdapter implements SearchEvidencePort {

    private static final String SYSTEM = "ai-worker/search";

    private final WebClient aiWorkerWebClient;

    public RagSearchAdapter(WebClient aiWorkerWebClient) {
        this.aiWorkerWebClient = aiWorkerWebClient;
    }

    @Override
    public Mono<List<Evidence>> search(EvidenceQuery query) {
        return aiWorkerWebClient.post()
                .uri("/search")
                .bodyValue(SearchRequestDto.from(query))
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> ExternalSystemException.of(
                                SYSTEM, "검색 응답 오류: %d".formatted(response.statusCode().value()), null)))
                .bodyToMono(SearchResponseDto.class)
                .map(SearchResponseDto::toDomain);
    }
}
