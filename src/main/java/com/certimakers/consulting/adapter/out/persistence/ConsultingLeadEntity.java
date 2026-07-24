package com.certimakers.consulting.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code consulting_lead} 테이블 매핑. {@code contactPhone}·{@code contactEmail}은 <b>암호화된</b>
 * 문자열을 담는다 — 매퍼가 암·복호화를 담당하고, 이 엔티티는 암호문을 그대로 저장한다.
 */
@Entity
@Table(name = "consulting_lead")
public class ConsultingLeadEntity {

    @Id
    private UUID id;

    @Column(name = "diagnosis_id", nullable = false)
    private UUID diagnosisId;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone; // 암호문

    @Column(name = "contact_email")
    private String contactEmail; // 암호문 (nullable)

    @Column
    private String message;

    @Column(name = "owner_user_id")
    private String ownerUserId;

    @Column(nullable = false)
    private String status;

    @Column(name = "assigned_consultant_id")
    private String assignedConsultantId;

    @Column(name = "internal_memo")
    private String internalMemo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToOne(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true,
            optional = false, fetch = FetchType.LAZY)
    private ConsentLogEntity consent;

    protected ConsultingLeadEntity() {
    }

    public ConsultingLeadEntity(
            UUID id, UUID diagnosisId, String contactName, String contactPhone,
            String contactEmail, String message, String ownerUserId, String status,
            Instant createdAt) {
        this.id = id;
        this.diagnosisId = diagnosisId;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.message = message;
        this.ownerUserId = ownerUserId;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** 워크플로 변경(상태·담당·메모)을 반영한다. 연락처·동의는 건드리지 않는다. */
    public void applyWorkflow(String status, String assignedConsultantId, String internalMemo) {
        this.status = status;
        this.assignedConsultantId = assignedConsultantId;
        this.internalMemo = internalMemo;
    }

    public String getAssignedConsultantId() {
        return assignedConsultantId;
    }

    public String getInternalMemo() {
        return internalMemo;
    }

    public void attachConsent(ConsentLogEntity consent) {
        consent.setLead(this);
        this.consent = consent;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDiagnosisId() {
        return diagnosisId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getMessage() {
        return message;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ConsentLogEntity getConsent() {
        return consent;
    }
}
