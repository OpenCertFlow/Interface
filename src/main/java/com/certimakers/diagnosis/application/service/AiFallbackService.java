package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.diagnosis.application.port.in.ManageAiFallbackUseCase;
import com.certimakers.diagnosis.application.port.out.AiFallbackSwitchPort;
import reactor.core.publisher.Mono;

/** AI 폴백 스위치 조회·설정. 인메모리 토글이라 블로킹이 없다. */
@UseCase
public class AiFallbackService implements ManageAiFallbackUseCase {

    private final AiFallbackSwitchPort switchPort;

    public AiFallbackService(AiFallbackSwitchPort switchPort) {
        this.switchPort = switchPort;
    }

    @Override
    public Mono<FallbackState> get() {
        return Mono.fromSupplier(() -> new FallbackState(
                switchPort.isEvidenceDisabled(), switchPort.isNarrationDisabled()));
    }

    @Override
    public Mono<FallbackState> update(boolean evidenceDisabled, boolean narrationDisabled) {
        return Mono.fromSupplier(() -> {
            switchPort.setEvidenceDisabled(evidenceDisabled);
            switchPort.setNarrationDisabled(narrationDisabled);
            return new FallbackState(evidenceDisabled, narrationDisabled);
        });
    }
}
