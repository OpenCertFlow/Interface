package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.GetDiagnosisReportQuery;
import com.certimakers.diagnosis.application.port.out.LoadDiagnosisPort;
import com.certimakers.diagnosis.domain.error.DiagnosisErrorCode;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
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
