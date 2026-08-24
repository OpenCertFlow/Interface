package io.opencertflow.diagnosis.application.port.out;

import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.PrepPlan;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 아웃바운드 포트: 준비계획 저장·조회. 블로킹(JPA). */
public interface PrepPlanPort {

    /** 진단당 계획은 하나이므로 진단 id로 찾는다. */
    Optional<PrepPlan> findByDiagnosisId(DiagnosisId diagnosisId);

    /**
     * 여러 진단의 준비계획을 한 번에 찾는다. 진단 이력 목록에서 진단마다 조회하면 N+1이 된다.
     *
     * @return 계획이 있는 진단만 담긴 맵. 트래커를 만들지 않은 진단은 키 자체가 없다
     */
    Map<DiagnosisId, PrepPlan> findByDiagnosisIds(List<DiagnosisId> diagnosisIds);

    PrepPlan save(PrepPlan plan);
}
