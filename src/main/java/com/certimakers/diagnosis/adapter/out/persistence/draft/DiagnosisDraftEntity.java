package com.certimakers.diagnosis.adapter.out.persistence.draft;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * {@code diagnosis_draft} 테이블 매핑(F-APP-004). 미완성 입력을 원문 그대로 담으므로 payload는
 * 구조화하지 않고 JSON 텍스트로 둔다. id는 애플리케이션이 전역 시퀀스로 부여한다.
 */
@Entity
@Table(name = "diagnosis_draft")
public class DiagnosisDraftEntity {

    @Id
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;

    @Column(name = "product_group")
    private String productGroup;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DiagnosisDraftEntity() {
    }

    public DiagnosisDraftEntity(
            Long id, String ownerUserId, String productGroup, String payload,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.productGroup = productGroup;
        this.payload = payload;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getProductGroup() {
        return productGroup;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
