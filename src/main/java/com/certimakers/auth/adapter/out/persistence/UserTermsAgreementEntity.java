package com.certimakers.auth.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_terms_agreement")
public class UserTermsAgreementEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "term_key", nullable = false)
    private String termKey;

    @Column(name = "term_version", nullable = false)
    private String termVersion;

    @Column(name = "agreed_at", nullable = false)
    private Instant agreedAt;

    protected UserTermsAgreementEntity() {
    }

    public UserTermsAgreementEntity(
            UUID id, UUID userId, String termKey, String termVersion, Instant agreedAt) {
        this.id = id;
        this.userId = userId;
        this.termKey = termKey;
        this.termVersion = termVersion;
        this.agreedAt = agreedAt;
    }
}
