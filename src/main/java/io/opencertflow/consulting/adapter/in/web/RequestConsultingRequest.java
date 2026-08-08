package io.opencertflow.consulting.adapter.in.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 상담 연결 요청 본문.
 *
 * <p>{@code privacyConsent}는 {@link AssertTrue}로 반드시 true여야 한다 — 웹 계층에서 1차로 막고,
 * 도메인({@code ConsultingLead.submit})이 2차로 강제한다. 방어가 두 겹이다.
 */
public record RequestConsultingRequest(
        @NotNull Long diagnosisId,
        @NotBlank String contactName,
        @NotBlank String contactPhone,
        String contactEmail,
        String message,
        @AssertTrue(message = "개인정보 수집·이용 동의가 필요합니다.") boolean privacyConsent,
        boolean sensitiveInfoConsent,
        boolean serviceLimitAcknowledged,
        @NotBlank String consentVersion) {
}
