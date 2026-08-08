package io.opencertflow.diagnosis.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.diagnosis.application.port.in.GetDiagnosisReportQuery;
import io.opencertflow.diagnosis.application.port.out.LoadDiagnosisPort;
import io.opencertflow.diagnosis.domain.error.DiagnosisErrorCode;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import reactor.core.publisher.Mono;

/** 진단 리포트 조회. 없으면 404. */
@UseCase
public class GetDiagnosisReportService implements GetDiagnosisReportQuery {

    private final LoadDiagnosisPort loadDiagnosisPort;
    private final BlockingBridge blockingBridge;

    public GetDiagnosisReportService(LoadDiagnosisPort loadDiagnosisPort, BlockingBridge blockingBridge) {
        this.loadDiagnosisPort = loadDiagnosisPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<Diagnosis> getById(DiagnosisId id) {
        return blockingBridge.mono(() -> loadDiagnosisPort.load(id))
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND)));
    }
}
