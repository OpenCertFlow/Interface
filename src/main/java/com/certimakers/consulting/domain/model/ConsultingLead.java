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
    private final Instant createdAt;
    private LeadStatus status;

    private ConsultingLead(
            ConsultingLeadId id, DiagnosisReference diagnosis, ContactInfo contact,
            String message, ConsentRecord consent, LeadStatus status, Instant createdAt) {
        this.id = Guard.notNull(id, "id");
        this.diagnosis = Guard.notNull(diagnosis, "diagnosis");
        this.contact = Guard.notNull(contact, "contact");
        this.message = message;
        this.consent = Guard.notNull(consent, "consent");
        this.status = Guard.notNull(status, "status");
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
            String message, ConsentRecord consent, Instant createdAt) {
        if (!consent.allowsProcessing()) {
            throw new BusinessException(ConsultingErrorCode.PRIVACY_CONSENT_REQUIRED);
        }
        return new ConsultingLead(
                id, diagnosis, contact, normalizeMessage(message), consent, LeadStatus.SUBMITTED, createdAt);
    }

    /** 저장된 상태에서 되살린다(영속성 재구성 전용). */
    public static ConsultingLead reconstitute(
            ConsultingLeadId id, DiagnosisReference diagnosis, ContactInfo contact,
            String message, ConsentRecord consent, LeadStatus status, Instant createdAt) {
        return new ConsultingLead(id, diagnosis, contact, message, consent, status, createdAt);
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

    public Instant createdAt() {
        return createdAt;
    }
}
