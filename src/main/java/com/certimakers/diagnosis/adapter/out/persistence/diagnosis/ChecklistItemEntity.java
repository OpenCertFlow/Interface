package com.certimakers.diagnosis.adapter.out.persistence.diagnosis;

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

/** {@code checklist_item} 테이블 매핑. weight는 평가 시점 스냅샷. */
@Entity
@Table(name = "checklist_item")
public class ChecklistItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "globalIdSeq")
    @SequenceGenerator(name = "globalIdSeq", sequenceName = "global_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private DiagnosisEntity diagnosis;

    @Column(name = "document_code", nullable = false)
    private String documentCode;

    @Column(nullable = false)
    private String requirement;

    @Column(nullable = false)
    private int weight;

    /** HELD · MISSING · UNKNOWN. '모름'을 '없음'으로 뭉개지 않기 위해 boolean이 아니다. */
    @Column(nullable = false, length = 16)
    private String status;

    protected ChecklistItemEntity() {
    }

    public ChecklistItemEntity(
            String documentCode, String requirement, int weight, String status) {
        this.documentCode = documentCode;
        this.requirement = requirement;
        this.weight = weight;
        this.status = status;
    }

    void setDiagnosis(DiagnosisEntity diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public String getRequirement() {
        return requirement;
    }

    public int getWeight() {
        return weight;
    }

    public String getStatus() {
        return status;
    }
}
