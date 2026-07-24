package com.certimakers.diagnosis.application.port.in;

import reactor.core.publisher.Mono;

/** AI 장애 폴백 설정(F-WADM-020). RAG 근거·LLM 문장화를 강제로 끄고 켠다. */
public interface ManageAiFallbackUseCase {

    Mono<FallbackState> get();

    Mono<FallbackState> update(boolean evidenceDisabled, boolean narrationDisabled);

    record FallbackState(boolean evidenceDisabled, boolean narrationDisabled) {
    }
}
