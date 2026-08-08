package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import reactor.core.publisher.Mono;

/** 인바운드 포트: 진단 ID로 저장된 리포트를 조회한다. */
public interface GetDiagnosisReportQuery {

    /**
     * @param viewerUserId 요청자. {@code null}이면 비로그인이다. 소유자가 있는 진단은 본인만
     *                     볼 수 있고, 그렇지 않으면 존재 여부를 알리지 않기 위해 404를 낸다.
     */
    Mono<Diagnosis> getById(DiagnosisId id, String viewerUserId);
}
