package io.opencertflow.diagnosis.adapter.out.persistence.prep;

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

/** {@code prep_plan_item} 테이블 매핑. 서류 하나의 확보 여부다. */
@Entity
@Table(name = "prep_plan_item")
public class PrepPlanItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "globalIdSeq")
    @SequenceGenerator(name = "globalIdSeq", sequenceName = "global_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prep_plan_id", nullable = false)
    private PrepPlanEntity plan;

    @Column(name = "document_code", nullable = false)
    private String documentCode;

    @Column(nullable = false)
    private boolean done;

    protected PrepPlanItemEntity() {
    }

    public PrepPlanItemEntity(String documentCode, boolean done) {
        this.documentCode = documentCode;
        this.done = done;
    }

    void setPlan(PrepPlanEntity plan) {
        this.plan = plan;
    }

    /** 더티 체킹으로 UPDATE를 낸다. 항목을 지웠다 다시 넣지 않기 위함이다. */
    void updateDone(boolean done) {
        this.done = done;
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public boolean isDone() {
        return done;
    }
}
