package com.certimakers.diagnosis.adapter.out.persistence.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** {@code document_weight} 테이블 매핑. 준비도 점수 가중치 기준표. */
@Entity
@Table(name = "document_weight")
public class DocumentWeightEntity {

    @Id
    @Column(name = "document_code")
    private String documentCode;

    @Column(nullable = false)
    private int weight;

    protected DocumentWeightEntity() {
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public int getWeight() {
        return weight;
    }
}
