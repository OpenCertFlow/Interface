package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.service.DiagnosisComparison;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import reactor.core.publisher.Mono;

/**
 * 재진단과 그 원 진단의 비교(F-APP-048). 조회 성격이라 UseCase가 아닌 Query로 둔다
 * (기존 {@code GetDiagnosisReportQuery}·{@code GetRemediationPlanQuery}와 같은 결).
 */
public interface CompareDiagnosisQuery {

    /**
     * 재진단 결과를 그 원 진단과 비교한다.
     *
     * @param id              재진단 ID. 부모는 서버가 previous_id로 찾는다
     * @param requesterUserId 요청자. 두 진단 모두 이 사용자 소유여야 한다
     */
    Mono<DiagnosisComparison> compare(DiagnosisId id, String requesterUserId);
}
