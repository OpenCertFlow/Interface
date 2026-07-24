package com.certimakers.report.adapter.out.persistence;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.report.application.port.out.ReportPhrasePort;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
public class ReportPhrasePersistenceAdapter implements ReportPhrasePort {

    private final ReportPhraseJpaRepository repository;

    public ReportPhrasePersistenceAdapter(ReportPhraseJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Phrase> findAll() {
        return repository.findAllByOrderByPhraseKeyAsc().stream()
                .map(e -> new Phrase(e.getPhraseKey(), e.getText(), e.getDescription()))
                .toList();
    }

    @Override
    @Transactional
    public void upsert(String phraseKey, String text, String description) {
        ReportPhraseEntity entity = repository.findById(phraseKey)
                .orElseGet(() -> new ReportPhraseEntity(phraseKey, text, description));
        entity.update(text, description);
        repository.save(entity);
    }
}
