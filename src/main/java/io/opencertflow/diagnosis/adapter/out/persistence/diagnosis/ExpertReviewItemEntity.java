package io.opencertflow.diagnosis.adapter.out.persistence.diagnosis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/** {@code expert_review_item} 테이블 매핑. */
@Entity
@Table(name = "expert_review_item")
public class ExpertReviewItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "globalIdSeq")
    @SequenceGenerator(name = "globalIdSeq", sequenceName = "global_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private DiagnosisEntity diagnosis;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false)
    private String reason;

    protected ExpertReviewItemEntity() {
    }

    public ExpertReviewItemEntity(String question, String reason) {
        this.question = question;
        this.reason = reason;
    }

    void setDiagnosis(DiagnosisEntity diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getQuestion() {
        return question;
    }

    public String getReason() {
        return reason;
    }
}
