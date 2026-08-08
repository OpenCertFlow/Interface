package io.opencertflow.diagnosis.adapter.out.persistence.diagnosis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** {@code certification_candidate} 테이블 매핑. */
@Entity
@Table(name = "certification_candidate")
public class CertificationCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "globalIdSeq")
    @SequenceGenerator(name = "globalIdSeq", sequenceName = "global_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private DiagnosisEntity diagnosis;

    @Column(name = "scheme_code", nullable = false)
    private String schemeCode;

    @Column(name = "certification_type", nullable = false)
    private String certificationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_rule_codes", nullable = false)
    private String matchedRuleCodes;

    protected CertificationCandidateEntity() {
    }

    public CertificationCandidateEntity(String schemeCode, String certificationType, String matchedRuleCodes) {
        this.schemeCode = schemeCode;
        this.certificationType = certificationType;
        this.matchedRuleCodes = matchedRuleCodes;
    }

    void setDiagnosis(DiagnosisEntity diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getSchemeCode() {
        return schemeCode;
    }

    public String getCertificationType() {
        return certificationType;
    }

    public String getMatchedRuleCodes() {
        return matchedRuleCodes;
    }
}
