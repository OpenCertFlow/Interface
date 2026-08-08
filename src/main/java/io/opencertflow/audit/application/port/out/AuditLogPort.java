package io.opencertflow.audit.application.port.out;

import java.time.Instant;
import java.util.List;

/** 감사 로그 저장·조회. 블로킹(JPA)이므로 호출자는 BlockingBridge로 감싼다. */
public interface AuditLogPort {

    void record(String actor, String httpMethod, String requestPath, Integer statusCode,
                Instant occurredAt);

    List<AuditRow> findRecent(String actorFilter, int limit);

    record AuditRow(String actor, String httpMethod, String requestPath, Integer statusCode,
                    Instant occurredAt) {
    }
}
