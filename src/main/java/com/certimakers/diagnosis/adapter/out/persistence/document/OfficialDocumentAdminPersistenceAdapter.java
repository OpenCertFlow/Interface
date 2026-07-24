package com.certimakers.diagnosis.adapter.out.persistence.document;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.diagnosis.application.port.out.OfficialDocumentAdminPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
public class OfficialDocumentAdminPersistenceAdapter implements OfficialDocumentAdminPort {

    private final OfficialDocumentJpaRepository repository;
    private final IdGenerator idGenerator;

    public OfficialDocumentAdminPersistenceAdapter(
            OfficialDocumentJpaRepository repository, IdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentRow> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentRow> findById(UUID id) {
        return repository.findById(id).map(this::toRow);
    }

    @Override
    @Transactional
    public UUID register(DocumentData data) {
        OfficialDocumentEntity entity = new OfficialDocumentEntity(
                idGenerator.nextId(), data.title(), data.issuer(), data.publishedAt(),
                data.verifiedAt(), data.productGroup(), data.certificationType(),
                data.schemeName(), data.sourceUrl());
        return repository.save(entity).getId();
    }

    @Override
    @Transactional
    public boolean update(UUID id, DocumentData data) {
        return repository.findById(id)
                .map(entity -> {
                    entity.update(
                            data.title(), data.issuer(), data.publishedAt(), data.verifiedAt(),
                            data.productGroup(), data.certificationType(), data.schemeName(),
                            data.sourceUrl());
                    repository.save(entity);
                    return true;
                })
                .orElse(false);
    }

    private DocumentRow toRow(OfficialDocumentEntity entity) {
        return new DocumentRow(
                entity.getId(), entity.getTitle(), entity.getIssuer(), entity.getPublishedAt(),
                entity.getVerifiedAt(), entity.getProductGroup(), entity.getCertificationType(),
                entity.getSchemeName(), entity.getSourceUrl(), entity.getCreatedAt());
    }
}
