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

/**
 * 진단 리포트 조회. 없거나 볼 권한이 없으면 404.
 *
 * <p><b>권한 없음도 404로 돌려준다.</b> 403은 "그 id는 존재한다"를 알려 주는 신호가 되어, 식별자를
 * 훑는 쪽에 존재 여부를 그대로 넘겨준다. 없는 것과 못 보는 것을 구분해 줄 이유가 없다.
 */
@UseCase
public class GetDiagnosisReportService implements GetDiagnosisReportQuery {

    private final LoadDiagnosisPort loadDiagnosisPort;
    private final BlockingBridge blockingBridge;

    public GetDiagnosisReportService(
            LoadDiagnosisPort loadDiagnosisPort, BlockingBridge blockingBridge) {
        this.loadDiagnosisPort = loadDiagnosisPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<Diagnosis> getById(DiagnosisId id, String viewerUserId) {
        return blockingBridge.mono(() -> loadDiagnosisPort.load(id))
                .filter(diagnosis -> diagnosis.isVisibleTo(viewerUserId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND)));
    }
}
