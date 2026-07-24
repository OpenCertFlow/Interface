package com.certimakers.consulting.adapter.out.persistence;

import com.certimakers.common.adapter.out.crypto.TextEncryptor;
import com.certimakers.consulting.domain.model.ConsentRecord;
import com.certimakers.consulting.domain.model.ConsultingLead;
import com.certimakers.consulting.domain.model.ConsultingLeadId;
import com.certimakers.consulting.domain.model.ContactInfo;
import com.certimakers.consulting.domain.model.DiagnosisReference;
import com.certimakers.consulting.domain.model.LeadStatus;

/**
 * 컨설팅 리드 도메인 ↔ 엔티티 매핑. <b>연락처 암·복호화가 여기서 일어난다.</b>
 *
 * <p>도메인은 평문 {@link ContactInfo}를 다루고, 이 매퍼가 저장 직전 phone·email을 암호화한다.
 * 암호화를 어댑터에 가둬 두면 도메인 로직과 테스트는 암호화를 신경 쓰지 않아도 된다.
 */
public class ConsultingLeadMapper {

    private final TextEncryptor encryptor;

    public ConsultingLeadMapper(TextEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    public ConsultingLeadEntity toEntity(ConsultingLead lead) {
        ContactInfo contact = lead.contact();
        ConsultingLeadEntity entity = new ConsultingLeadEntity(
                lead.id().value(),
                lead.diagnosis().value(),
                contact.name(),
                encryptor.encrypt(contact.phone()),                       // 암호화
                contact.hasEmail() ? encryptor.encrypt(contact.email()) : null, // 암호화
                lead.message().orElse(null),
                lead.ownerUserId().orElse(null),
                lead.status().name(),
                lead.createdAt());

        ConsentRecord consent = lead.consent();
        entity.attachConsent(new ConsentLogEntity(
                lead.diagnosis().value(),
                consent.privacyConsent(),
                consent.sensitiveInfoConsent(),
                consent.serviceLimitAcknowledged(),
                consent.consentVersion(),
                lead.createdAt()));
        return entity;
    }

    public ConsultingLead toDomain(ConsultingLeadEntity entity) {
        ConsentLogEntity consentEntity = entity.getConsent();
        ContactInfo contact = new ContactInfo(
                entity.getContactName(),
                encryptor.decrypt(entity.getContactPhone()),              // 복호화
                entity.getContactEmail() != null ? encryptor.decrypt(entity.getContactEmail()) : null);
        ConsentRecord consent = new ConsentRecord(
                consentEntity.isPrivacyConsent(),
                consentEntity.isSensitiveInfoConsent(),
                consentEntity.isServiceLimitAcknowledged(),
                consentEntity.getConsentVersion());
        return ConsultingLead.reconstitute(
                ConsultingLeadId.of(entity.getId()),
                DiagnosisReference.of(entity.getDiagnosisId()),
                contact,
                entity.getMessage(),
                consent,
                entity.getOwnerUserId(),
                LeadStatus.valueOf(entity.getStatus()),
                entity.getAssignedConsultantId(),
                entity.getInternalMemo(),
                entity.getCreatedAt());
    }
}
