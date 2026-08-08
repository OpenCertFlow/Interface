package io.opencertflow.auth.adapter.out.persistence;

import io.opencertflow.auth.application.port.out.TermsPort;
import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.common.domain.port.IdGenerator;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
public class TermsPersistenceAdapter implements TermsPort {

    private final TermsJpaRepository termsRepository;
    private final UserTermsAgreementJpaRepository agreementRepository;
    private final IdGenerator idGenerator;

    public TermsPersistenceAdapter(
            TermsJpaRepository termsRepository,
            UserTermsAgreementJpaRepository agreementRepository, IdGenerator idGenerator) {
        this.termsRepository = termsRepository;
        this.agreementRepository = agreementRepository;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Term> loadActive() {
        return termsRepository.findByActiveIsTrueOrderByRequiredDescTermKeyAsc().stream()
                .map(e -> new Term(
                        e.getTermKey(), e.getVersion(), e.getTitle(), e.getContent(), e.isRequired()))
                .toList();
    }

    @Override
    @Transactional
    public void saveAgreements(Long userId, List<AgreedTerm> agreed, Instant agreedAt) {
        agreed.forEach(term -> agreementRepository.save(new UserTermsAgreementEntity(
                idGenerator.nextId(), userId, term.termKey(), term.version(), agreedAt)));
    }
}
