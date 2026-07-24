package com.certimakers.audit.application.port.in;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/** 감사 로그 조회(F-WADM-018). */
public interface QueryAuditLogUseCase {

    Mono<List<AuditView>> recent(String actorFilter, int limit);

    record AuditView(String actor, String httpMethod, String requestPath, Integer statusCode,
                     Instant occurredAt) {
    }
}
