package com.certimakers.consulting.application.port.out;

import com.certimakers.consulting.domain.model.ConsultingLead;

/**
 * 아웃바운드 포트: 컨설팅 리드를 저장한다. 블로킹(JPA).
 *
 * <p>연락처는 저장 어댑터가 암호화한다 — 도메인은 평문만 안다. 리드와 동의 기록을 한 트랜잭션에
 * 함께 저장한다.
 */
public interface SaveConsultingLeadPort {

    ConsultingLead save(ConsultingLead lead);
}
