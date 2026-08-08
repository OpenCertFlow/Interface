package io.opencertflow.auth.application.service;

import io.opencertflow.auth.application.port.in.GetTermsUseCase;
import io.opencertflow.auth.application.port.out.TermsPort;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
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
