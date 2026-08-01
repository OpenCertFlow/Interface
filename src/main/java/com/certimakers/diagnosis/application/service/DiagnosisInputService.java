package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.GetDiagnosisInputQuery;
import com.certimakers.diagnosis.application.port.out.LoadDiagnosisPort;
import com.certimakers.diagnosis.domain.error.DiagnosisErrorCode;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import reactor.core.publisher.Mono;

/**
 * 재진단 화면 프리필용 이전 입력 조회(F-APP-034).
 *
 * <p>진단에 저장된 {@code ProductProfile}을 그대로 돌려줄 뿐 계산이 없다. 소유권 확인만 한다.
 */
@UseCase
public class DiagnosisInputService implements GetDiagnosisInputQuery {

    private final LoadDiagnosisPort loadDiagnosisPort;
    private final BlockingBridge blockingBridge;

    public DiagnosisInputService(
            LoadDiagnosisPort loadDiagnosisPort, BlockingBridge blockingBridge) {
        this.loadDiagnosisPort = loadDiagnosisPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<ProductProfile> getInput(DiagnosisId id, String requesterUserId) {
        return blockingBridge.mono(() -> loadDiagnosisPort.load(id))
                .filter(diagnosis -> diagnosis.owner()
                        .map(owner -> owner.equals(requesterUserId))
                        .orElse(false))       // 익명 진단은 소유자가 없으므로 항상 거부
                .map(Diagnosis::profile)
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND)));
    }
}