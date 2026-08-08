package io.opencertflow.diagnosis.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.error.CommonErrorCode;
import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.ManageDiagnosisDraftUseCase;
import io.opencertflow.diagnosis.application.port.out.DiagnosisDraftPort;
import io.opencertflow.diagnosis.domain.model.DiagnosisDraft;
import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 진단 입력 초안 관리(F-APP-004). 소유자 본인으로만 제한하며, 없거나 남의 초안은 '찾을 수 없음'으로
 * 다룬다(존재를 드러내지 않는다). 미완성 입력을 그대로 보존하므로 진단 검증은 하지 않는다.
 */
@UseCase
public class DiagnosisDraftService implements ManageDiagnosisDraftUseCase {

    private final DiagnosisDraftPort draftPort;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final BlockingBridge blockingBridge;

    public DiagnosisDraftService(
            DiagnosisDraftPort draftPort, IdGenerator idGenerator,
            TimeProvider timeProvider, BlockingBridge blockingBridge) {
        this.draftPort = draftPort;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<DiagnosisDraft> create(String ownerUserId, String productGroup, String payload) {
        Instant now = timeProvider.now();
        // id 생성(전역 시퀀스 nextval)은 블로킹 JDBC라 블로킹 스케줄러 안에서 얻는다.
        return blockingBridge.mono(() -> draftPort.save(new DiagnosisDraft(
                idGenerator.nextId(), ownerUserId, productGroup, payload, now, now)));
    }

    @Override
    public Mono<DiagnosisDraft> update(
            long id, String ownerUserId, String productGroup, String payload) {
        return blockingBridge.mono(() -> {
            DiagnosisDraft existing = loadOwned(id, ownerUserId);
            return draftPort.save(new DiagnosisDraft(
                    existing.id(), existing.ownerUserId(), productGroup, payload,
                    existing.createdAt(), timeProvider.now()));
        });
    }

    @Override
    public Mono<List<DiagnosisDraft>> listMine(String ownerUserId) {
        return blockingBridge.mono(() -> draftPort.findByOwner(ownerUserId));
    }

    @Override
    public Mono<DiagnosisDraft> get(long id, String ownerUserId) {
        return blockingBridge.mono(() -> loadOwned(id, ownerUserId));
    }

    @Override
    public Mono<Void> delete(long id, String ownerUserId) {
        return blockingBridge.<Void>mono(() -> {
            loadOwned(id, ownerUserId);
            draftPort.deleteById(id);
            return null;
        });
    }

    /** 소유자 본인의 초안만 돌려준다. 없거나 남의 것이면 404. */
    private DiagnosisDraft loadOwned(long id, String ownerUserId) {
        return draftPort.findById(id)
                .filter(draft -> draft.ownerUserId().equals(ownerUserId))
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }
}
