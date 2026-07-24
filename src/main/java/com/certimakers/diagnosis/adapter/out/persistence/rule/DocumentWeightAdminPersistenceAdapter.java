package com.certimakers.diagnosis.adapter.out.persistence.rule;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.diagnosis.application.port.out.DocumentWeightAdminPort;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
public class DocumentWeightAdminPersistenceAdapter implements DocumentWeightAdminPort {

    private final DocumentWeightJpaRepository repository;

    public DocumentWeightAdminPersistenceAdapter(DocumentWeightJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeightRow> findAll() {
        return repository.findAll().stream()
                .map(entity -> new WeightRow(
                        entity.getDocumentCode(), entity.getDisplayName(),
                        entity.getRequirement(), entity.getWeight(), entity.getNote()))
                .sorted((a, b) -> Integer.compare(b.weight(), a.weight()))
                .toList();
    }

    @Override
    @Transactional
    public boolean adjust(String documentCode, int weight, String note) {
        return repository.findById(documentCode)
                .map(entity -> {
                    entity.adjust(weight, note);
                    repository.save(entity);
                    return true;
                })
                .orElse(false);
    }
}
