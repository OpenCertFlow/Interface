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

/** {@code diagnosis_evidence} 테이블 매핑. source_url은 NOT NULL(불변식 6). */
@Entity
@Table(name = "diagnosis_evidence")
public class EvidenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "globalIdSeq")
    @SequenceGenerator(name = "globalIdSeq", sequenceName = "global_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private DiagnosisEntity diagnosis;

    @Column(name = "source_document_id", nullable = false)
    private String sourceDocumentId;

    @Column(name = "section_type", nullable = false)
    private String sectionType;

    @Column(nullable = false)
    private String snippet;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(nullable = false)
    private double relevance;

    protected EvidenceEntity() {
    }

    public EvidenceEntity(
            String sourceDocumentId, String sectionType, String snippet, String sourceUrl, double relevance) {
        this.sourceDocumentId = sourceDocumentId;
        this.sectionType = sectionType;
        this.snippet = snippet;
        this.sourceUrl = sourceUrl;
        this.relevance = relevance;
    }

    void setDiagnosis(DiagnosisEntity diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getSourceDocumentId() {
        return sourceDocumentId;
    }

    public String getSectionType() {
        return sectionType;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public double getRelevance() {
        return relevance;
    }
}
