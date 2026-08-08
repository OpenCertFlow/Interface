package io.opencertflow.diagnosis.adapter.out.persistence.diagnosis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** {@code narration} 테이블 매핑. 진단과 0:1이며 PK를 공유한다. */
@Entity
@Table(name = "narration")
public class NarrationEntity {

    @Id
    @Column(name = "diagnosis_id")
    private Long diagnosisId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "diagnosis_id")
    private DiagnosisEntity diagnosis;

    @Column(nullable = false)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_actions", nullable = false)
    private String nextActions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pre_consult_questions", nullable = false)
    private String preConsultQuestions;

    @Column(nullable = false)
    private String disclaimer;

    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Column(name = "is_template_fallback", nullable = false)
    private boolean templateFallback;

    protected NarrationEntity() {
    }

    public NarrationEntity(
            String summary, String nextActions, String preConsultQuestions,
            String disclaimer, String modelId, boolean templateFallback) {
        this.summary = summary;
        this.nextActions = nextActions;
        this.preConsultQuestions = preConsultQuestions;
        this.disclaimer = disclaimer;
        this.modelId = modelId;
        this.templateFallback = templateFallback;
    }

    void setDiagnosis(DiagnosisEntity diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getSummary() {
        return summary;
    }

    public String getNextActions() {
        return nextActions;
    }

    public String getPreConsultQuestions() {
        return preConsultQuestions;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public String getModelId() {
        return modelId;
    }

    public boolean isTemplateFallback() {
        return templateFallback;
    }
}
