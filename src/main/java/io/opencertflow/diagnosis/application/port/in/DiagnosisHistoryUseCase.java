package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 내 진단 이력 조회·재진단·삭제(F-APP-032/034/035). 모두 <b>소유자 본인</b>에 한정된다.
 * 남의 진단이나 익명 진단은 이 경로로 보이지도, 지워지지도 않는다.
 */
public interface DiagnosisHistoryUseCase {

    /**
     * 내 진단을 최신순으로 조회한다. 준비 트래커(F-APP-049)를 만든 진단은 진행률이 함께 온다 —
     * 앱이 목록에서 준비 중인 진단을 식별해 트래커로 바로 들어갈 수 있어야 한다.
     */
    Mono<List<DiagnosisHistoryEntry>> listMine(String ownerUserId);

    /**
     * 확인·수정한 입력으로 기존 진단을 다시 평가해 새 진단을 만든다(재진단). 새 진단도 요청자 소유다.
     *
     * @param updatedProfile 앱이 이전 입력을 채운 폼에서 제출한 입력. 제품군은 원 진단과 같아야 한다
     */
    Mono<Diagnosis> rediagnose(
            DiagnosisId id, String requesterUserId, ProductProfile updatedProfile);

    /** 내 진단을 삭제한다. 소유자가 아니거나 없으면 찾을 수 없음으로 다룬다. */
    Mono<Void> delete(DiagnosisId id, String requesterUserId);
}
