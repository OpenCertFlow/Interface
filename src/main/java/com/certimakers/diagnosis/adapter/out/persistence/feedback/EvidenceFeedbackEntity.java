package com.certimakers.diagnosis.adapter.out.persistence.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evidence_feedback")
public class EvidenceFeedbackEntity {

    @Id
    private Long id;

    @Column(name = "diagnosis_id", nullable = false)
    private Long diagnosisId;

    @Column(name = "source_document_id", nullable = false)
    private String sourceDocumentId;

    @Column(name = "section_type")
    private String sectionType;

    @Column(nullable = false)
    private String verdict;

    @Column
    private String comment;

    @Column(name = "reported_by", nullable = false)
    private String reportedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected EvidenceFeedbackEntity() {
    }

    public EvidenceFeedbackEntity(Long id, Long diagnosisId, String sourceDocumentId,
                                  String sectionType, String verdict, String comment,
                                  String reportedBy) {
        this.id = id;
        this.diagnosisId = diagnosisId;
        this.sourceDocumentId = sourceDocumentId;
        this.sectionType = sectionType;
        this.verdict = verdict;
        this.comment = comment;
        this.reportedBy = reportedBy;
    }

    public String getSourceDocumentId() {
        return sourceDocumentId;
    }

    public String getVerdict() {
        return verdict;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
