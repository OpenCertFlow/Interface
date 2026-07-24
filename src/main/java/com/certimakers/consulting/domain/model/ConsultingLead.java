package com.certimakers.consulting.domain.model;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.model.AggregateRoot;
import com.certimakers.common.domain.model.Guard;
import com.certimakers.consulting.domain.error.ConsultingErrorCode;
import java.time.Instant;
import java.util.Optional;

/**
 * 컨설팅 리드 애그리거트. 진단 결과에 연결된 상담 요청이다.
 *
 * <p>이것은 진단과 <b>별도 애그리거트</b>다. {@link DiagnosisReference}로 진단을 참조만 하고, 컨설팅
 * 상태 변경이 진단 애그리거트를 잠그지 않는다(04-domain-model.md). "진단 결과 기반 리드 커넥션
 * 모델"이 이 서비스의 사업화 차별성이다(기획서).
 */
public class ConsultingLead extends AggregateRoot<ConsultingLeadId> {

    private final ConsultingLeadId id;
    private final DiagnosisReference diagnosis;
    private final ContactInfo contact;
    private final String message;
    private final ConsentRecord consent;
    private final String ownerUserId;
    private final Instant createdAt;
    private LeadStatus status;
    private String assignedConsultantId;
    private String internalMemo;

    private ConsultingLead(
            ConsultingLeadId id, DiagnosisReference diagnosis, ContactInfo contact,
            String message, ConsentRecord consent, String ownerUserId, LeadStatus status,
            String assignedConsultantId, String internalMemo, Instant createdAt) {
        this.id = Guard.notNull(id, "id");
        this.diagnosis = Guard.notNull(diagnosis, "diagnosis");
        this.contact = Guard.notNull(contact, "contact");
        this.message = message;
        this.consent = Guard.notNull(consent, "consent");
        this.ownerUserId = ownerUserId;
        this.status = Guard.notNull(status, "status");
        this.assignedConsultantId = assignedConsultantId;
        this.internalMemo = internalMemo;
        this.createdAt = Guard.notNull(createdAt, "createdAt");
    }

    /**
     * 새 상담 신청을 접수한다. 개인정보 수집·이용 동의가 없으면 거부한다.
     *
     * <p>이 검증이 아키텍처의 개인정보 보호선이다 — 동의 없는 연락처는 애초에 애그리거트로 만들어지지
     * 않으므로 저장될 수 없다.
     */
    public static ConsultingLead submit(
            ConsultingLeadId id, DiagnosisReference diagnosis, ContactInfo contact,
            String message, ConsentRecord consent, String ownerUserId, Instant createdAt) {
        if (!consent.allowsProcessing()) {
            throw new BusinessException(ConsultingErrorCode.PRIVACY_CONSENT_REQUIRED);
        }
        return new ConsultingLead(
                id, diagnosis, contact, normalizeMessage(message), consent, ownerUserId,
                LeadStatus.SUBMITTED, null, null, createdAt);
    }

    /** 저장된 상태에서 되살린다(영속성 재구성 전용). */
    public static ConsultingLead reconstitute(
            ConsultingLeadId id, DiagnosisReference diagnosis, ContactInfo contact,
            String message, ConsentRecord consent, String ownerUserId, LeadStatus status,
            String assignedConsultantId, String internalMemo, Instant createdAt) {
        return new ConsultingLead(
                id, diagnosis, contact, message, consent, ownerUserId, status,
                assignedConsultantId, internalMemo, createdAt);
    }

    /** 담당 컨설턴트를 배정한다. 접수 상태면 배정 상태로 넘어가고, 이미 진행 중이면 담당만 바꾼다. */
    public void assignTo(String consultantId) {
        Guard.hasText(consultantId, "consultantId");
        requireNotTerminal();
        this.assignedConsultantId = consultantId;
        if (status == LeadStatus.SUBMITTED) {
            this.status = LeadStatus.ASSIGNED;
        }
    }

    /** 상태를 전이한다. 허용되지 않은 전이는 거부한다. */
    public void transitionTo(LeadStatus next) {
        Guard.notNull(next, "status");
        if (status == next) {
            return;
        }
        if (!status.canTransitionTo(next)) {
            throw new BusinessException(
                    ConsultingErrorCode.INVALID_STATUS_TRANSITION,
                    "%s에서 %s로 바꿀 수 없습니다.".formatted(status, next));
        }
        if (next == LeadStatus.IN_PROGRESS && assignedConsultantId == null) {
            throw new BusinessException(
                    ConsultingErrorCode.INVALID_STATUS_TRANSITION, "담당 배정 후 진행할 수 있습니다.");
        }
        this.status = next;
    }

    /** 컨설턴트 내부 메모를 남긴다. 사용자에게 공개되지 않는다. */
    public void updateInternalMemo(String memo) {
        this.internalMemo = (memo == null || memo.isBlank()) ? null : memo.trim();
    }

    private void requireNotTerminal() {
        if (status.isTerminal()) {
            throw new BusinessException(
                    ConsultingErrorCode.INVALID_STATUS_TRANSITION, "종료된 상담은 변경할 수 없습니다.");
        }
    }

    private static String normalizeMessage(String message) {
        return (message == null || message.isBlank()) ? null : message.trim();
    }

    @Override
    public ConsultingLeadId id() {
        return id;
    }

    public DiagnosisReference diagnosis() {
        return diagnosis;
    }

    public ContactInfo contact() {
        return contact;
    }

    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    public ConsentRecord consent() {
        return consent;
    }

    public LeadStatus status() {
        return status;
    }

    public Optional<String> ownerUserId() {
        return Optional.ofNullable(ownerUserId);
    }

    public Optional<String> assignedConsultantId() {
        return Optional.ofNullable(assignedConsultantId);
    }

    public Optional<String> internalMemo() {
        return Optional.ofNullable(internalMemo);
    }

    public Instant createdAt() {
        return createdAt;
    }
}
