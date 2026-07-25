package com.certimakers.auth.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsAgreementJpaRepository
        extends JpaRepository<UserTermsAgreementEntity, UUID> {
}
