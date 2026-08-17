package io.opencertflow.diagnosis.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.ManageRuleSetUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 룰셋 읽기 전용 API(F-WCON-004 근거·Rule 상세). 컨설턴트·관리자가 진단 근거가 된 룰을 조회한다.
 *
 * <p>편집은 관리자 전용(/api/v1/admin/rule-sets)이고, 이 경로는 조회만 열어 CONSULTANT도 볼 수 있게 한다.
 */
@Tag(name = "룰셋 조회", description = "적용된 판정 규칙 열람")
@WebAdapter
@RequestMapping("/api/v1/rule-sets")
public class RuleSetReadController {

    private final ManageRuleSetUseCase manageRuleSetUseCase;
    private final TimeProvider timeProvider;

    public RuleSetReadController(
            ManageRuleSetUseCase manageRuleSetUseCase, TimeProvider timeProvider) {
        this.manageRuleSetUseCase = manageRuleSetUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ManageRuleSetUseCase.RuleSetSummary>>>> list() {
        return manageRuleSetUseCase.list().flatMap(body -> wrap(body));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<ManageRuleSetUseCase.RuleSetDetail>>> get(
            @PathVariable String id) {
        return manageRuleSetUseCase.get(Long.parseLong(id)).flatMap(body -> wrap(body));
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body) {
        return TraceId.current().map(traceId ->
                ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
