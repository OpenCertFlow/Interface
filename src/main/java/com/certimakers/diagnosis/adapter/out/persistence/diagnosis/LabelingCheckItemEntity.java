package com.certimakers.diagnosis.adapter.out.persistence.diagnosis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** {@code labeling_check_item} 테이블 매핑. */
@Entity
@Table(name = "labeling_check_item")
public class LabelingCheckItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private DiagnosisEntity diagnosis;

    @Column(nullable = false)
    private String label;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_rule_codes", nullable = false)
    private String matchedRuleCodes;

    protected LabelingCheckItemEntity() {
    }

    public LabelingCheckItemEntity(String label, String matchedRuleCodes) {
        this.label = label;
        this.matchedRuleCodes = matchedRuleCodes;
    }

    void setDiagnosis(DiagnosisEntity diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getLabel() {
        return label;
    }

    public String getMatchedRuleCodes() {
        return matchedRuleCodes;
    }
}
