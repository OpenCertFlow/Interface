package io.opencertflow.consulting.application.port.out;

import io.opencertflow.consulting.domain.model.ConsultingLead;

/**
 * 컨설팅 리드 워크플로 변경(상태·담당·메모) 저장. 연락처·동의는 재저장하지 않는다 —
 * 기존 행의 워크플로 컬럼만 갱신한다.
 */
public interface UpdateConsultingLeadPort {

    void update(ConsultingLead lead);
}
