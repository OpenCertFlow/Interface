package io.opencertflow.report.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.report.application.port.in.ManageReportPhraseUseCase;
import io.opencertflow.report.application.port.out.ReportPhrasePort;
import java.util.List;
import reactor.core.publisher.Mono;

@UseCase
public class ReportPhraseService implements ManageReportPhraseUseCase {

    private final ReportPhrasePort phrasePort;
    private final BlockingBridge blockingBridge;

    public ReportPhraseService(ReportPhrasePort phrasePort, BlockingBridge blockingBridge) {
        this.phrasePort = phrasePort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<List<PhraseView>> list() {
        return blockingBridge.mono(() -> phrasePort.findAll().stream()
                .map(p -> new PhraseView(p.phraseKey(), p.text(), p.description()))
                .toList());
    }

    @Override
    public Mono<Void> update(String phraseKey, String text, String description) {
        return Mono.fromSupplier(() -> {
            if (phraseKey == null || phraseKey.isBlank()) {
                throw BusinessException.invalid("문구 키가 필요합니다.");
            }
            if (text == null || text.isBlank()) {
                throw BusinessException.invalid("문구 내용이 필요합니다.");
            }
            return phraseKey.strip();
        }).flatMap(key -> blockingBridge.run(() -> phrasePort.upsert(key, text.strip(), description)));
    }
}
