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

/** {@code checklist_item} 테이블 매핑. weight는 평가 시점 스냅샷. */
@Entity
@Table(name = "checklist_item")
public class ChecklistItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private DiagnosisEntity diagnosis;

    @Column(name = "document_code", nullable = false)
    private String documentCode;

    @Column(nullable = false)
    private String requirement;

    @Column(nullable = false)
    private int weight;

    @Column(nullable = false)
    private boolean held;

    protected ChecklistItemEntity() {
    }

    public ChecklistItemEntity(String documentCode, String requirement, int weight, boolean held) {
        this.documentCode = documentCode;
        this.requirement = requirement;
        this.weight = weight;
        this.held = held;
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

    public boolean isHeld() {
        return held;
    }
}
