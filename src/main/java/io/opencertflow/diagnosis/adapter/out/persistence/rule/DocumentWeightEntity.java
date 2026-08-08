package io.opencertflow.diagnosis.adapter.out.persistence.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_weight")
public class DocumentWeightEntity {

    @Id
    @Column(name = "document_code")
    private String documentCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String requirement;

    @Column(nullable = false)
    private int weight;

    @Column
    private String note;

    protected DocumentWeightEntity() {
    }

    /**
     * 정의 파일({@code weights/document-weights.yaml})에서 기본값을 채울 때 쓴다.
     *
     * <p>운영 중 값 변경은 {@link #adjust(int, String)}만 허용한다 — 코드·표시명·요구 강도는
     * 서류의 정체성이라 한번 정해지면 바뀌지 않는다.
     */
    public DocumentWeightEntity(
            String documentCode, String displayName, String requirement, int weight, String note) {
        this.documentCode = documentCode;
        this.displayName = displayName;
        this.requirement = requirement;
        this.weight = weight;
        this.note = note;
    }

    // 가중치·비고만 조정한다. 코드·표시명·요구 강도는 서류의 정체성이라 바꾸지 않는다.
    public void adjust(int weight, String note) {
        this.weight = weight;
        this.note = note;
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRequirement() {
        return requirement;
    }

    public int getWeight() {
        return weight;
    }

    public String getNote() {
        return note;
    }
}
