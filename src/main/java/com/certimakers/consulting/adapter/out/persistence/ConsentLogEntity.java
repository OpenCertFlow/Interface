package com.certimakers.consulting.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * {@code consent_log} 테이블 매핑. 리드와 1:1이지만 자체 {@code id} PK를 가진다(ERD).
 * {@code consulting_lead_id}는 FK 조인 컬럼이며, 이쪽이 관계의 소유측이다.
 */
@Entity
@Table(name = "consent_log")
public class ConsentLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "globalIdSeq")
    @SequenceGenerator(name = "globalIdSeq", sequenceName = "global_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consulting_lead_id", nullable = false)
    private ConsultingLeadEntity lead;

    @Column(name = "diagnosis_id", nullable = false)
    private Long diagnosisId;

    @Column(name = "privacy_consent", nullable = false)
    private boolean privacyConsent;

    @Column(name = "sensitive_info_consent", nullable = false)
    private boolean sensitiveInfoConsent;

    @Column(name = "service_limit_acknowledged", nullable = false)
    private boolean serviceLimitAcknowledged;

    @Column(name = "consent_version", nullable = false)
    private String consentVersion;

    @Column(name = "consented_at", nullable = false)
    private Instant consentedAt;

    protected ConsentLogEntity() {
    }

    public ConsentLogEntity(
            Long diagnosisId, boolean privacyConsent, boolean sensitiveInfoConsent,
            boolean serviceLimitAcknowledged, String consentVersion, Instant consentedAt) {
        this.diagnosisId = diagnosisId;
        this.privacyConsent = privacyConsent;
        this.sensitiveInfoConsent = sensitiveInfoConsent;
        this.serviceLimitAcknowledged = serviceLimitAcknowledged;
        this.consentVersion = consentVersion;
        this.consentedAt = consentedAt;
    }

    void setLead(ConsultingLeadEntity lead) {
        this.lead = lead;
    }

    public boolean isPrivacyConsent() {
        return privacyConsent;
    }

    public boolean isSensitiveInfoConsent() {
        return sensitiveInfoConsent;
    }

    public boolean isServiceLimitAcknowledged() {
        return serviceLimitAcknowledged;
    }

    public String getConsentVersion() {
        return consentVersion;
    }
}
