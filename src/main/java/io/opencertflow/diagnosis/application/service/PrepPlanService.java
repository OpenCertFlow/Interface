package io.opencertflow.diagnosis.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.ManagePrepPlanUseCase;
import io.opencertflow.diagnosis.application.port.out.LoadDiagnosisPort;
import io.opencertflow.diagnosis.application.port.out.PrepPlanPort;
import io.opencertflow.diagnosis.domain.error.DiagnosisErrorCode;
import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.PrepPlan;
import io.opencertflow.diagnosis.domain.model.PrepPlanId;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 인증 준비 트래커(F-APP-049).
 *
 * <p>진단을 <b>읽기만</b> 하고 수정하지 않는다 — 진단은 평가 시점의 스냅샷이다. 진행률 계산은
 * {@link PrepPlan}(도메인)이 하고, 여기서는 로드·소유권·저장만 다룬다.
 *
 * <p>세 메서드 모두 {@code blockingBridge.mono(...)} 안에서 동기 코드로 쓴다. 흐름이 전부 DB
 * 작업이라 타임아웃·폴백·병렬이 필요 없고, 진단을 읽어야 소유권을 알고 소유권을 알아야 계획을
 * 만들 수 있어 애초에 순차다. {@code AuthService.login}이 같은 방식이다.
 */
@UseCase
public class PrepPlanService implements ManagePrepPlanUseCase {

    private final PrepPlanPort prepPlanPort;
    private final LoadDiagnosisPort loadDiagnosisPort;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public PrepPlanService(
            PrepPlanPort prepPlanPort,
            LoadDiagnosisPort loadDiagnosisPort,
            BlockingBridge blockingBridge,
            IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.prepPlanPort = prepPlanPort;
        this.loadDiagnosisPort = loadDiagnosisPort;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<PrepPlan> createOrGet(DiagnosisId diagnosisId, String requesterUserId) {
        return blockingBridge.mono(() -> {
            Diagnosis diagnosis = loadOwnedDiagnosis(diagnosisId, requesterUserId);
            return prepPlanPort.findByDiagnosisId(diagnosisId)
                    .orElseGet(() -> prepPlanPort.save(newPlanFrom(diagnosis, requesterUserId)));
        });
    }

    @Override
    public Mono<PrepPlan> get(DiagnosisId diagnosisId, String requesterUserId) {
        return blockingBridge.mono(() -> loadOwnedPlan(diagnosisId, requesterUserId));
    }

    @Override
    public Mono<PrepPlan> check(
            DiagnosisId diagnosisId, DocumentCode documentCode, boolean done, String requesterUserId) {
        return blockingBridge.mono(() -> {
            PrepPlan plan = loadOwnedPlan(diagnosisId, requesterUserId);
            plan.check(documentCode, done, timeProvider.now());   // 목록에 없으면 도메인이 거부
            return prepPlanPort.save(plan);
        });
    }

    /**
     * 진단의 누락 서류로 목록을 만든다. 준비도 점수는 건드리지 않는다.
     *
     * <p>{@code checklist()}를 직접 거르지 않고 {@code remediationOrder()}를 쓴다 — 누락 필터와
     * <b>보완 우선순위(가중치 내림차순)</b>를 진단이 이미 정해 두었기 때문이다. 트래커야말로
     * "무엇부터 준비할까"에 답해야 하는 화면이라 이 순서가 그대로 표시 순서가 된다.
     *
     * <p>'모름'({@code UNKNOWN})도 목록에 들어간다 — {@code isMissing()}이 HELD가 아닌 것을
     * 모두 포함하기 때문이며, 의도한 대로다. 확인해서 체크하는 것도 준비 과정이고, 빼면 모름
     * 항목이 어느 화면에도 나타나지 않는다.
     */
    private PrepPlan newPlanFrom(Diagnosis diagnosis, String ownerUserId) {
        List<DocumentCode> missing = diagnosis.remediationOrder().stream()
                .map(ChecklistItem::documentCode)
                .toList();
        return PrepPlan.from(
                PrepPlanId.of(idGenerator.nextId()),
                ownerUserId,
                diagnosis.id(),
                missing,
                timeProvider.now());
    }

    /**
     * 소유자 본인의 진단만 돌려준다. 없거나 남의 것이면 404(존재를 드러내지 않는다).
     *
     * <p>{@code DiagnosisHistoryService}·{@code DiagnosisCompareService}에 같은 이름의 private
     * 메서드가 있지만 꺼내 쓸 수 없다 — 서비스끼리 참조하면 의존이 얽히므로 각자 포트로 로드한다.
     */
    private Diagnosis loadOwnedDiagnosis(DiagnosisId id, String requesterUserId) {
        Diagnosis diagnosis = loadDiagnosisPort.load(id);
        boolean owned = diagnosis != null && diagnosis.owner()
                .map(owner -> owner.equals(requesterUserId))
                .orElse(false);            // 익명 진단은 소유자가 없으므로 항상 거부
        if (!owned) {
            throw new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND);
        }
        return diagnosis;
    }

    private PrepPlan loadOwnedPlan(DiagnosisId diagnosisId, String requesterUserId) {
        return prepPlanPort.findByDiagnosisId(diagnosisId)
                .filter(plan -> plan.isOwnedBy(requesterUserId))
                .orElseThrow(() ->
                        new BusinessException(DiagnosisErrorCode.DIAGNOSIS_NOT_FOUND));
    }
}
