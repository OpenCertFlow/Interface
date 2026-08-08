package io.opencertflow.consulting.application.port.in;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.consulting.domain.model.ConsentRecord;
import io.opencertflow.consulting.domain.model.ContactInfo;
import io.opencertflow.consulting.domain.model.DiagnosisReference;

/**
 * 컨설팅 연결 요청. 웹 어댑터가 검증·변환해 만든다.
 *
 * @param diagnosis    연결할 진단
 * @param contact      상담 연락처 (평문 — 저장 시 암호화됨)
 * @param message      상담 요청 메시지 (선택)
 * @param consent      동의 기록
 * @param ownerUserId  접수한 로그인 사용자 id. 비로그인 접수면 null(알림 수신자가 없다)
 */
public record RequestConsultingCommand(
        DiagnosisReference diagnosis,
        ContactInfo contact,
        String message,
        ConsentRecord consent,
        String ownerUserId) {

    public RequestConsultingCommand {
        Guard.notNull(diagnosis, "diagnosis");
        Guard.notNull(contact, "contact");
        Guard.notNull(consent, "consent");
    }
}
