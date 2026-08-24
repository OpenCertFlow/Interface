package io.opencertflow.diagnosis.adapter.out.persistence.prep;

import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.diagnosis.application.port.out.PrepPlanPort;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.PrepItem;
import io.opencertflow.diagnosis.domain.model.PrepPlan;
import io.opencertflow.diagnosis.domain.model.PrepPlanId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/** 인증 준비 트래커(F-APP-049) 영속 어댑터. 블로킹(JPA)이며 트랜잭션 경계가 이 안에 있다. */
@PersistenceAdapter
public class PrepPlanPersistenceAdapter implements PrepPlanPort {

    private final PrepPlanJpaRepository repository;

    public PrepPlanPersistenceAdapter(PrepPlanJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrepPlan> findByDiagnosisId(DiagnosisId diagnosisId) {
        return repository.findByDiagnosisId(diagnosisId.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<DiagnosisId, PrepPlan> findByDiagnosisIds(List<DiagnosisId> diagnosisIds) {
        if (diagnosisIds.isEmpty()) {
            return Map.of();   // in () 은 SQL 문법 오류라 질의 자체를 보내지 않는다
        }
        List<Long> ids = diagnosisIds.stream().map(DiagnosisId::value).toList();
        return repository.findAllWithItemsByDiagnosisIdIn(ids).stream()
                .map(this::toDomain)
                .collect(Collectors.toMap(PrepPlan::diagnosisId, plan -> plan));
    }

    /**
     * 새 계획이면 통째로 저장하고, 이미 있으면 <b>항목의 done만 갱신</b>한다.
     *
     * <p>{@link PrepPlan}에는 항목을 추가·삭제하는 방법이 없다 — 생성 시점에 고정되고
     * {@code check}로 done만 바뀐다. 매번 새 엔티티로 갈아끼우면 체크 한 번에 항목 전체가
     * DELETE·INSERT 된다.
     */
    @Override
    @Transactional
    public PrepPlan save(PrepPlan plan) {
        PrepPlanEntity entity = repository.findById(plan.id().value())
                .map(existing -> updateDoneFlags(existing, plan))
                .orElseGet(() -> repository.save(toEntity(plan)));
        return toDomain(entity);
    }

    /** 더티 체킹으로 UPDATE가 나간다. save를 다시 부르지 않는다. */
    private PrepPlanEntity updateDoneFlags(PrepPlanEntity entity, PrepPlan plan) {
        Map<String, Boolean> desired = plan.items().stream()
                .collect(Collectors.toMap(
                        item -> item.documentCode().value(), PrepItem::done));
        entity.getItems().forEach(item ->
                item.updateDone(desired.getOrDefault(item.getDocumentCode(), item.isDone())));
        entity.touch(plan.updatedAt());
        return entity;
    }

    private PrepPlanEntity toEntity(PrepPlan plan) {
        PrepPlanEntity entity = new PrepPlanEntity(
                plan.id().value(),
                plan.ownerUserId(),
                plan.diagnosisId().value(),
                plan.createdAt(),
                plan.updatedAt());
        entity.attachItems(plan.items().stream()
                .map(item -> new PrepPlanItemEntity(item.documentCode().value(), item.done()))
                .toList());
        return entity;
    }

    private PrepPlan toDomain(PrepPlanEntity entity) {
        List<PrepItem> items = entity.getItems().stream()
                .map(item -> PrepItem.reconstitute(
                        DocumentCode.of(item.getDocumentCode()), item.isDone()))
                .toList();
        return PrepPlan.reconstitute(
                PrepPlanId.of(entity.getId()),
                entity.getOwnerUserId(),
                DiagnosisId.of(entity.getDiagnosisId()),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
