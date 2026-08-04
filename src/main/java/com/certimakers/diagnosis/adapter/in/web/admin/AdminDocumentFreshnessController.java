package com.certimakers.diagnosis.adapter.in.web.admin;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.MonitorDocumentFreshnessUseCase;
import com.certimakers.diagnosis.application.port.in.MonitorDocumentFreshnessUseCase.CheckSummary;
import com.certimakers.diagnosis.application.port.in.MonitorDocumentFreshnessUseCase.StaleDocumentView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 공식 문서 원문 변경 감지 운영 API.
 *
 * <p>우리는 공식 자료를 자동으로 갱신하지 않는다(운영지침 §9.2). 대신 원문이 달라졌는지를
 * 감지해 관리자에게 재검토를 요청한다. 무엇을 어떻게 고칠지는 사람이 원문을 읽고 정한다.
 */
@WebAdapter
@RequestMapping("/api/v1/admin/document-freshness")
public class AdminDocumentFreshnessController {

    private final MonitorDocumentFreshnessUseCase monitorUseCase;
    private final TimeProvider timeProvider;

    public AdminDocumentFreshnessController(
            MonitorDocumentFreshnessUseCase monitorUseCase, TimeProvider timeProvider) {
        this.monitorUseCase = monitorUseCase;
        this.timeProvider = timeProvider;
    }

    /** 재검토가 필요한 문서 목록. 관리자 큐다. */
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<StaleDocumentView>>>> pending() {
        return monitorUseCase.pendingReview().flatMap(this::wrap);
    }

    /** 지금 즉시 원문을 훑는다. 스케줄을 기다리지 않고 확인하고 싶을 때 쓴다. */
    @PostMapping("/checks")
    public Mono<ResponseEntity<ApiResponse<CheckSummary>>> checkNow() {
        return monitorUseCase.checkAll().flatMap(this::wrap);
    }

    /** 재검토 완료. 변경 표시를 지운다. */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> markReviewed(@PathVariable String id) {
        return monitorUseCase.markReviewed(Long.parseLong(id))
                .then(Mono.defer(() -> this.<Void>wrap(null)));
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body) {
        return TraceId.current().map(traceId ->
                ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
