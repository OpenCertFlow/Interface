package com.certimakers.auth.application.service;

import com.certimakers.auth.application.port.in.GetTermsUseCase;
import com.certimakers.auth.application.port.out.TermsPort;
import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import java.util.List;
import reactor.core.publisher.Mono;

@UseCase
public class TermsService implements GetTermsUseCase {

    private final TermsPort termsPort;
    private final BlockingBridge blockingBridge;

    public TermsService(TermsPort termsPort, BlockingBridge blockingBridge) {
        this.termsPort = termsPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<List<TermView>> current() {
        return blockingBridge.mono(() -> termsPort.loadActive().stream()
                .map(t -> new TermView(t.termKey(), t.version(), t.title(), t.content(), t.required()))
                .toList());
    }
}
