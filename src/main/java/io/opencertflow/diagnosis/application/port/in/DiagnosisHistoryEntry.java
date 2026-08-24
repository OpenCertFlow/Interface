package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.model.DiagnosisSummary;
import io.opencertflow.diagnosis.domain.model.PrepPlan;

/**
 * 진단 이력 목록의 한 줄. 진단 요약과 <b>준비 현황</b>을 함께 낸다(F-APP-032 + F-APP-049).
 *
 * <p>둘은 다른 애그리거트라 하나로 합칠 수 없다 — {@link DiagnosisSummary}에 진행률을 넣으면
 * 진단 영속 어댑터가 준비계획 테이블까지 읽어야 한다. 합치는 일은 애플리케이션이 한다.
 *
 * @param prepPlan 준비 트래커를 아직 만들지 않았으면 null
 */
public record DiagnosisHistoryEntry(DiagnosisSummary summary, PrepPlan prepPlan) {
}
