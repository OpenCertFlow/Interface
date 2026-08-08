package io.opencertflow.auth.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsAgreementJpaRepository
        extends JpaRepository<UserTermsAgreementEntity, Long> {
}
