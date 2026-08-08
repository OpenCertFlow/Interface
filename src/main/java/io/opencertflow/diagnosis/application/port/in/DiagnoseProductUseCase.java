package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.model.Diagnosis;
import reactor.core.publisher.Mono;

/**
 * 인바운드 포트: 제품 정보를 받아 진단을 실행하고 완결된 결과를 돌려준다.
 *
 * <p>반환되는 {@link Diagnosis}는 항상 종결 상태(COMPLETED 또는 COMPLETED_DEGRADED)다. 룰셋 로드나
 * 저장이 실패하면 정상 값이 아니라 오류로 끝난다 — 이 둘만이 폴백 없는 실패 지점이다(03-diagnosis-flow.md).
 */
public interface DiagnoseProductUseCase {

    Mono<Diagnosis> diagnose(DiagnoseCommand command);
}
