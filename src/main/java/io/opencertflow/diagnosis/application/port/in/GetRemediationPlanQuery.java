package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.simulation.RemediationPlan;
import reactor.core.publisher.Mono;

/** 목표 준비도에 도달하기 위한 최소 보완 경로 조회. */
public interface GetRemediationPlanQuery {

    Mono<RemediationPlan> plan(DiagnosisId diagnosisId, int targetScore);
}
