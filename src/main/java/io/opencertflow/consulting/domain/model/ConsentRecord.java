package io.opencertflow.consulting.domain.model;

import io.opencertflow.common.domain.model.Guard;

/**
 * 상담 신청 시 받은 동의 기록. 감사 추적을 위해 진단 결과와 함께 저장된다(consent_log).
 *
 * <p>개인정보 수집·이용 동의({@code privacyConsent})는 필수다 — 없으면 리드를 만들 수 없다. 나머지는
 * 신뢰성·고지 목적으로 함께 기록한다. {@code consentVersion}은 어떤 버전의 약관에 동의했는지를 남긴다.
 *
 * @param privacyConsent            개인정보 수집·이용 동의 (필수)
 * @param sensitiveInfoConsent      민감 제조정보 처리 동의
 * @param serviceLimitAcknowledged  서비스 한계 고지 확인
 * @param consentVersion            동의한 약관 버전
 */
public record ConsentRecord(
        boolean privacyConsent,
        boolean sensitiveInfoConsent,
        boolean serviceLimitAcknowledged,
        String consentVersion) {

    public ConsentRecord {
        Guard.hasText(consentVersion, "consentVersion");
    }

    public boolean allowsProcessing() {
        return privacyConsent;
    }
}
