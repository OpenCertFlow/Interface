package com.certimakers.diagnosis.application.port.out;

import com.certimakers.diagnosis.domain.model.Diagnosis;

/**
 * 아웃바운드 포트: 진단 애그리거트를 통째로 저장한다. 블로킹(JPA).
 *
 * <p>저장 실패는 폴백이 없다 — 룰셋 로드와 함께 진단을 실패시키는 두 지점 중 하나다. 트랜잭션
 * 경계는 구현 어댑터 안에서 시작하고 끝난다. 애플리케이션 서비스에 {@code @Transactional}을 붙이면
 * 리액티브 체인에서 동작하지 않는다(ADR-0002).
 */
public interface SaveDiagnosisPort {

    Diagnosis save(Diagnosis diagnosis);
}
