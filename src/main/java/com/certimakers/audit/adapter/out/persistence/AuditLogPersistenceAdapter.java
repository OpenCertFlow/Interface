package com.certimakers.audit.adapter.out.persistence;

import com.certimakers.audit.application.port.out.AuditLogPort;
import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.common.domain.port.IdGenerator;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
public class AuditLogPersistenceAdapter implements AuditLogPort {

    private final AuditLogJpaRepository repository;
    private final IdGenerator idGenerator;

    public AuditLogPersistenceAdapter(AuditLogJpaRepository repository, IdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public void record(String actor, String httpMethod, String requestPath, Integer statusCode,
                       Instant occurredAt) {
        repository.save(new AuditLogEntity(
                idGenerator.nextId(), actor, httpMethod, requestPath, statusCode, occurredAt));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditRow> findRecent(String actorFilter, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        List<AuditLogEntity> entities = actorFilter == null || actorFilter.isBlank()
                ? repository.findByOrderByOccurredAtDesc(page)
                : repository.findByActorOrderByOccurredAtDesc(actorFilter, page);
        return entities.stream()
                .map(e -> new AuditRow(
                        e.getActor(), e.getHttpMethod(), e.getRequestPath(),
                        e.getStatusCode(), e.getOccurredAt()))
                .toList();
    }
}
