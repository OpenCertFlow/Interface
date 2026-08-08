package io.opencertflow.diagnosis.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.application.port.in.DiagnoseCommand;
import io.opencertflow.diagnosis.application.port.in.DiagnoseProductUseCase;
import io.opencertflow.diagnosis.application.port.in.DiagnosisHistoryUseCase;
import io.opencertflow.diagnosis.application.port.out.DiagnosisHistoryPort;
import io.opencertflow.diagnosis.application.port.out.LoadDiagnosisPort;
import io.opencertflow.diagnosis.domain.error.DiagnosisErrorCode;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.DiagnosisSummary;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 내 진단 이력 조회·재진단·삭제(F-APP-032/034/035).
 *
 * <p>모든 경로가 <b>소유자 본인</b>으로 제한된다. 소유자가 아니거나 익명(소유자 없음) 진단은 존재를
 * 드러내지 않도록 '찾을 수 없음'(404)으로 다룬다.
 */
@UseCase
public class DiagnosisHistoryService implements DiagnosisHistoryUseCase {

    /** 목록 상한. 소공인 한 명의 진단 수는 많지 않아 페이지네이션 없이 최신 N개만 준다. */
    private static final int MAX_HISTORY = 100;

    private final DiagnosisHistoryPort historyPort;
    private final LoadDiagnosisPort loadDiagnosisPort;
    private final DiagnoseProductUseCase diagnoseProductUseCase;
    private final BlockingBridge blockingBridge;

    public DiagnosisHistoryService(
            DiagnosisHistoryPort historyPort,
            LoadDiagnosisPort loadDiagnosisPort,
            DiagnoseProductUseCase diagnoseProductUseCase,
            BlockingBridge blockingBridge) {
        this.historyPort = historyPort;
        this.loadDiagnosisPort = loadDiagnosisPort;
        this.diagnoseProductUseCase = diagnoseProductUseCase;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<List<DiagnosisSummary>> listMine(String ownerUserId) {
        return blockingBridge.mono(() -> historyPort.findByOwner(ownerUserId, MAX_HISTORY));
    }

    @Override
    public Mono<Diagnosis> rediagnose(
            DiagnosisId id, String requesterUserId, ProductProfile updatedProfile) {
        Guard.notNull(updatedProfile, "updatedProfile");
        return loadOwned(id, requesterUserId)
                .flatMap(diagnosis -> {
                    // 제품군은 바꿀 수 없다 — 다르면 요구 서류 집합 자체가 달라 '같은 제품을 다시
                    // 진단한 것'이 아니고, 비교(F-APP-048)도 동일 제품군을 요구한다.
                    if (updatedProfile.productGroup() != diagnosis.profile().productGroup()) {
                        throw new BusinessException(DiagnosisErrorCode.PRODUCT_GROUP_CHANGED);
                    }
                    // 원 진단을 부모로 기록한다 — 이 한 줄이 있어야 나중에 비교가 가능하다.
                    return diagnoseProductUseCase.diagnose(DiagnoseCommand.rediagnosis(
                            updatedProfile, requesterUserId, diagnosis.id()));
                });
    }

    @Override
    public Mono<Void> delete(DiagnosisId id, String requesterUserId) {
        return loadOwned(id, requesterUserId)
                .flatMap(diagnosis -> blockingBridge.run(() -> historyPort.deleteById(id)));
    }

    /** 소유자 본인의 진단만 돌려준다. 없거나 남의 것이면 404(존재를 드러내지 않는다). */
    private Mono<Diagnosis> loadOwned(DiagnosisId id, String requesterUserId) {
        return blockingBridge.mono(() -> loadDiagnosisPort.load(id))
                .filter(diagnosis -> diagnosis.owner()
                        .map(owner -> owner.equals(requesterUserId))
                        .orElse(false))
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND)));
    }
}
