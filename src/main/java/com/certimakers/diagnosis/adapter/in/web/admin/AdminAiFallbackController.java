package com.certimakers.diagnosis.adapter.in.web.admin;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.ManageAiFallbackUseCase;
import com.certimakers.diagnosis.application.port.in.ManageAiFallbackUseCase.FallbackState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 관리자 AI 장애 폴백 설정 API(F-WADM-020). 외부 AI가 불안정할 때 RAG·LLM 호출을 강제로 꺼
 * 진단이 타임아웃을 기다리지 않고 결정론 결과로 즉시 응답하게 한다. 판정은 룰이 하므로 정확성은 유지.
 */
@WebAdapter
@RequestMapping("/api/v1/admin/ai-fallback")
public class AdminAiFallbackController {

    private final ManageAiFallbackUseCase manageAiFallbackUseCase;
    private final TimeProvider timeProvider;

    public AdminAiFallbackController(
            ManageAiFallbackUseCase manageAiFallbackUseCase, TimeProvider timeProvider) {
        this.manageAiFallbackUseCase = manageAiFallbackUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<FallbackState>>> get() {
        return manageAiFallbackUseCase.get().flatMap(this::wrap);
    }

    @PutMapping
    public Mono<ResponseEntity<ApiResponse<FallbackState>>> update(
            @RequestBody FallbackRequest request) {
        return manageAiFallbackUseCase.update(request.evidenceDisabled(), request.narrationDisabled())
                .flatMap(this::wrap);
    }

    public record FallbackRequest(boolean evidenceDisabled, boolean narrationDisabled) {
    }

    private Mono<ResponseEntity<ApiResponse<FallbackState>>> wrap(FallbackState body) {
        return TraceId.current().map(traceId ->
                ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
