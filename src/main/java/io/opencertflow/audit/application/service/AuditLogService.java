package io.opencertflow.audit.application.service;

import io.opencertflow.audit.application.port.in.QueryAuditLogUseCase;
import io.opencertflow.audit.application.port.in.RecordAuditUseCase;
import io.opencertflow.audit.application.port.out.AuditLogPort;
import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.common.domain.port.TimeProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** 감사 로그 기록·조회. 기록 실패는 로그만 남기고 삼킨다 — 감사가 원 요청을 막아선 안 된다. */
@UseCase
public class AuditLogService implements RecordAuditUseCase, QueryAuditLogUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 100;

    private final AuditLogPort auditLogPort;
    private final BlockingBridge blockingBridge;
    private final TimeProvider timeProvider;

    public AuditLogService(
            AuditLogPort auditLogPort, BlockingBridge blockingBridge, TimeProvider timeProvider) {
        this.auditLogPort = auditLogPort;
        this.blockingBridge = blockingBridge;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<Void> record(AuditCommand command) {
        return blockingBridge.run(() -> auditLogPort.record(
                        command.actor(), command.httpMethod(), command.requestPath(),
                        command.statusCode(), timeProvider.now()))
                .onErrorResume(error -> {
                    log.warn("감사 로그 기록 실패 — 원 요청은 계속한다. path={}, cause={}",
                            command.requestPath(), error.toString());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<List<AuditView>> recent(String actorFilter, int limit) {
        int capped = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return blockingBridge.mono(() -> auditLogPort.findRecent(actorFilter, capped).stream()
                .map(row -> new AuditView(
                        row.actor(), row.httpMethod(), row.requestPath(),
                        row.statusCode(), row.occurredAt()))
                .toList());
    }
}
