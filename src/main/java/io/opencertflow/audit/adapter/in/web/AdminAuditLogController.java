package io.opencertflow.audit.adapter.in.web;

import io.opencertflow.audit.application.port.in.QueryAuditLogUseCase;
import io.opencertflow.audit.application.port.in.QueryAuditLogUseCase.AuditView;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/** 관리자 감사 로그 조회 API(F-WADM-018). 조회(GET)는 감사 대상이 아니라 기록되지 않는다. */
@WebAdapter
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditLogController {

    private final QueryAuditLogUseCase queryAuditLogUseCase;
    private final TimeProvider timeProvider;

    public AdminAuditLogController(
            QueryAuditLogUseCase queryAuditLogUseCase, TimeProvider timeProvider) {
        this.queryAuditLogUseCase = queryAuditLogUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<AuditView>>>> list(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false, defaultValue = "100") int limit) {

        return queryAuditLogUseCase.recent(actor, limit)
                .flatMap(body -> TraceId.current().map(traceId ->
                        ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now()))));
    }
}
