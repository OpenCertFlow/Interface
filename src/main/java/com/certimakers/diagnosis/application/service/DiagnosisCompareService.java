package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.CompareDiagnosisQuery;
import com.certimakers.diagnosis.application.port.out.LoadDiagnosisPort;
import com.certimakers.diagnosis.domain.error.DiagnosisErrorCode;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.service.DiagnosisComparator;
import com.certimakers.diagnosis.domain.service.DiagnosisComparison;
import reactor.core.publisher.Mono;

/**
 * 재진단 결과 비교(F-APP-048).
 *
 * <p>하는 일은 <b>진단 두 개를 소유권 확인하며 꺼내 오는 것</b>뿐이다. 무엇이 나아졌는지 판단은
 * {@link DiagnosisComparator}(도메인 서비스)가 한다.
 */
@UseCase
public class DiagnosisCompareService implements CompareDiagnosisQuery {

    // 상태 없는 순수 함수라 주입받지 않고 직접 생성한다 —
    // DiagnoseProductService가 RuleEvaluator·ScoreCalculator를 다루는 방식과 같다.
    private final DiagnosisComparator comparator = new DiagnosisComparator();

    private final LoadDiagnosisPort loadDiagnosisPort;
    private final BlockingBridge blockingBridge;

    public DiagnosisCompareService(
            LoadDiagnosisPort loadDiagnosisPort, BlockingBridge blockingBridge) {
        this.loadDiagnosisPort = loadDiagnosisPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<DiagnosisComparison> compare(DiagnosisId id, String requesterUserId) {
        return loadOwned(id, requesterUserId)                        // ① 재진단을 꺼낸다(본인 것만)
                .flatMap(current -> current.previousDiagnosisId()    // ② 부모 id를 따라간다
                        .map(previousId -> loadOwned(previousId, requesterUserId)
                                // ③ 부모도 본인 것이어야 한다. 꺼내진 둘을 도메인에 넘긴다.
                                .map(previous -> comparator.compare(previous, current)))
                        // ④ 부모가 없으면 최초 진단이라 비교 대상이 없다.
                        .orElseGet(() -> Mono.error(
                                new BusinessException(DiagnosisErrorCode.NOT_COMPARABLE))));
    }

    /**
     * 소유자 본인의 진단만 돌려준다. 없거나 남의 것이면 404(존재를 드러내지 않는다).
     *
     * <p>{@code DiagnosisHistoryService}에 같은 이름의 private 메서드가 있지만 꺼내 쓸 수 없다 —
     * 서비스끼리 참조하면 의존이 얽히므로 각자 포트를 통해 로드한다.
     */
    private Mono<Diagnosis> loadOwned(DiagnosisId id, String requesterUserId) {
        return blockingBridge.mono(() -> loadDiagnosisPort.load(id))
                .filter(diagnosis -> diagnosis.owner()
                        .map(owner -> owner.equals(requesterUserId))
                        .orElse(false))       // 익명 진단은 소유자가 없으므로 항상 거부
                .switchIfEmpty(Mono.error(
                        new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND)));
    }
}