package com.certimakers.consulting.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.consulting.domain.error.ConsultingErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConsultingLeadTest {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

    private ConsultingLeadId id() {
        return ConsultingLeadId.of(UUID.randomUUID());
    }

    private DiagnosisReference diagnosis() {
        return DiagnosisReference.of(UUID.randomUUID());
    }

    private ContactInfo contact() {
        return new ContactInfo("홍길동", "010-1234-5678", "hong@example.com");
    }

    @Test
    @DisplayName("개인정보 동의가 있으면 SUBMITTED 상태로 접수된다")
    void 동의하면_접수() {
        ConsentRecord consent = new ConsentRecord(true, true, true, "v1");

        ConsultingLead lead = ConsultingLead.submit(
                id(), diagnosis(), contact(), "상담 원해요", consent, "owner-1", NOW);

        assertThat(lead.status()).isEqualTo(LeadStatus.SUBMITTED);
        assertThat(lead.message()).contains("상담 원해요");
        assertThat(lead.ownerUserId()).contains("owner-1");
    }

    @Test
    @DisplayName("개인정보 동의 없이는 리드를 만들 수 없다 — 아키텍처의 개인정보 보호선")
    void 동의없으면_거부() {
        ConsentRecord noConsent = new ConsentRecord(false, false, false, "v1");

        assertThatThrownBy(() -> ConsultingLead.submit(id(), diagnosis(), contact(), null, noConsent, null, NOW))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.errorCode()).isEqualTo(ConsultingErrorCode.PRIVACY_CONSENT_REQUIRED));
    }

    @Test
    @DisplayName("연락처는 마스킹되어 노출된다 — 뒷자리·앞글자만")
    void 연락처_마스킹() {
        ContactInfo info = new ContactInfo("홍길동", "010-1234-5678", "hong@example.com");

        assertThat(info.maskedPhone()).isEqualTo("****5678");
        assertThat(info.maskedEmail()).isEqualTo("ho***@example.com");
    }

    @Test
    @DisplayName("이메일은 선택 — 없어도 접수된다")
    void 이메일_없이_접수() {
        ContactInfo noEmail = new ContactInfo("홍길동", "010-1234-5678", null);
        ConsentRecord consent = new ConsentRecord(true, false, true, "v1");

        ConsultingLead lead = ConsultingLead.submit(id(), diagnosis(), noEmail, null, consent, null, NOW);

        assertThat(lead.contact().hasEmail()).isFalse();
        assertThat(lead.contact().maskedEmail()).isNull();
    }
}
