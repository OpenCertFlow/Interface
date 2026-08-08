package io.opencertflow.consulting.adapter.in.web;

/**
 * 상담 신청 확인 응답. 개인정보 노출을 최소화하기 위해 연락처는 <b>마스킹</b>해 돌려준다.
 *
 * @param id           리드 식별자
 * @param diagnosisId  연결된 진단
 * @param status       처리 상태
 * @param maskedPhone  가려진 전화번호 (예: ****1234)
 * @param maskedEmail  가려진 이메일 (선택)
 */
public record ConsultingLeadResponse(
        String id,
        String diagnosisId,
        String status,
        String maskedPhone,
        String maskedEmail) {
}
