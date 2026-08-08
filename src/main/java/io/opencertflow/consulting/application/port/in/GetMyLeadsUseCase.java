package io.opencertflow.consulting.application.port.in;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/** 소공인이 자신이 접수한 상담을 조회한다(F-APP-041). 소유자 연결이 있어야 가능하다. */
public interface GetMyLeadsUseCase {

    Mono<List<MyLeadView>> myLeads(String ownerUserId, int limit);

    record MyLeadView(String id, String diagnosisId, String status, Instant createdAt) {
    }
}
