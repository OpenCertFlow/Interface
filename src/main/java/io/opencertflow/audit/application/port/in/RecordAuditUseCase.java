package io.opencertflow.audit.application.port.in;

import reactor.core.publisher.Mono;

/** 감사 로그 기록(F-BE-018). 실패해도 원 요청을 깨뜨리지 않는다. */
public interface RecordAuditUseCase {

    Mono<Void> record(AuditCommand command);

    record AuditCommand(String actor, String httpMethod, String requestPath, Integer statusCode) {
    }
}
