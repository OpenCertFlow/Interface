package io.opencertflow.diagnosis.application.port.in;

import io.opencertflow.diagnosis.domain.model.DiagnosisDraft;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 진단 입력 초안 관리(F-APP-004). 저장해 두고 이어서 작성·수정하는 제품 입력을 다룬다.
 * 모든 경로가 소유자 본인에 한정된다 — 남의 초안은 보이지도, 바뀌지도, 지워지지도 않는다.
 */
public interface ManageDiagnosisDraftUseCase {

    Mono<DiagnosisDraft> create(String ownerUserId, String productGroup, String payload);

    Mono<DiagnosisDraft> update(long id, String ownerUserId, String productGroup, String payload);

    Mono<List<DiagnosisDraft>> listMine(String ownerUserId);

    Mono<DiagnosisDraft> get(long id, String ownerUserId);

    Mono<Void> delete(long id, String ownerUserId);
}
