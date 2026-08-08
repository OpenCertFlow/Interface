package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import reactor.core.publisher.Mono;

/** 인바운드 포트: 진단 ID로 저장된 리포트를 조회한다. */
public interface GetDiagnosisReportQuery {

    Mono<Diagnosis> getById(DiagnosisId id);
}
