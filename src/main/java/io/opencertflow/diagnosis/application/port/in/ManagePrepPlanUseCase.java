package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.PrepPlan;
import reactor.core.publisher.Mono;

/**
 * 인증 준비 트래커(F-APP-049). 모두 <b>소유자 본인</b>에 한정된다.
 * 남의 진단이나 익명 진단은 이 경로로 보이지도, 바뀌지도 않는다.
 */
public interface ManagePrepPlanUseCase {

    /** 진단의 누락 서류로 준비목록을 만든다. 이미 있으면 그것을 돌려준다(멱등). */
    Mono<PrepPlan> createOrGet(DiagnosisId diagnosisId, String requesterUserId);

    /** 현재 준비 현황을 조회한다. 목록이 없으면 찾을 수 없음으로 다룬다. */
    Mono<PrepPlan> get(DiagnosisId diagnosisId, String requesterUserId);

    /** 항목을 체크·해제한다. 목록에 없는 서류 코드는 거부한다. */
    Mono<PrepPlan> check(
            DiagnosisId diagnosisId, DocumentCode documentCode, boolean done, String requesterUserId);
}
