package io.opencertflow.diagnosis.adapter.out.persistence.draft;

import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.diagnosis.application.port.out.DiagnosisDraftPort;
import io.opencertflow.diagnosis.domain.model.DiagnosisDraft;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** 진단 초안(F-APP-004) 영속 어댑터. 블로킹(JPA)이며 트랜잭션 경계가 이 안에 있다. */
@PersistenceAdapter
public class DiagnosisDraftPersistenceAdapter implements DiagnosisDraftPort {

    private final DiagnosisDraftJpaRepository repository;

    public DiagnosisDraftPersistenceAdapter(DiagnosisDraftJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DiagnosisDraft save(DiagnosisDraft draft) {
        repository.save(toEntity(draft));
        return draft;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiagnosisDraft> findById(long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosisDraft> findByOwner(String ownerUserId) {
        return repository.findByOwnerUserIdOrderByUpdatedAtDesc(ownerUserId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        repository.deleteById(id);
    }

    private DiagnosisDraftEntity toEntity(DiagnosisDraft draft) {
        return new DiagnosisDraftEntity(
                draft.id(), draft.ownerUserId(), draft.productGroup(), draft.payload(),
                draft.createdAt(), draft.updatedAt());
    }

    private DiagnosisDraft toDomain(DiagnosisDraftEntity entity) {
        return new DiagnosisDraft(
                entity.getId(), entity.getOwnerUserId(), entity.getProductGroup(),
                entity.getPayload(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
