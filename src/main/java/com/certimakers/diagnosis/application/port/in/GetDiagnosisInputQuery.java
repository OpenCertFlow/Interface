package com.certimakers.diagnosis.application.port.in;

import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import reactor.core.publisher.Mono;

/**
 * 재진단 화면을 채우기 위한 이전 입력 조회(F-APP-034, 이슈 #15).
 *
 * <p>조회만 하므로 UseCase가 아닌 Query로 둔다. 본인 소유 진단만 돌려준다.
 */
public interface GetDiagnosisInputQuery {

    /** 진단에 사용된 입력을 돌려준다. 없거나 남의 것이면 찾을 수 없음으로 다룬다. */
    Mono<ProductProfile> getInput(DiagnosisId id, String requesterUserId);
}