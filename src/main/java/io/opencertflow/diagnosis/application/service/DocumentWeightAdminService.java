package io.opencertflow.diagnosis.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.diagnosis.application.port.in.ManageDocumentWeightUseCase;
import io.opencertflow.diagnosis.application.port.out.DocumentWeightAdminPort;
import java.util.List;
import reactor.core.publisher.Mono;

/** 준비도 가중치 조회·편집(F-WADM-011). 편집 대상은 가중치·비고뿐 — 코드·요구 강도는 서류의 정체성이라 두지 않는다. */
@UseCase
public class DocumentWeightAdminService implements ManageDocumentWeightUseCase {

    private final DocumentWeightAdminPort weightPort;
    private final BlockingBridge blockingBridge;

    public DocumentWeightAdminService(
            DocumentWeightAdminPort weightPort, BlockingBridge blockingBridge) {
        this.weightPort = weightPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<List<WeightView>> list() {
        return blockingBridge.mono(() -> weightPort.findAll().stream()
                .map(row -> new WeightView(
                        row.documentCode(), row.displayName(), row.requirement(),
                        row.weight(), row.note()))
                .toList());
    }

    @Override
    public Mono<Void> update(String documentCode, int weight, String note) {
        return Mono.fromSupplier(() -> {
            if (weight <= 0) {
                throw BusinessException.invalid("가중치는 1 이상이어야 합니다.");
            }
            return documentCode;
        }).flatMap(code -> blockingBridge.mono(() -> weightPort.adjust(code, weight, note)))
                .flatMap(found -> Boolean.TRUE.equals(found)
                        ? Mono.empty()
                        : Mono.error(BusinessException.invalid(
                                "가중치 기준에 없는 서류 코드입니다: " + documentCode)));
    }
}
