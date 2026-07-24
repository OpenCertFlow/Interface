package com.certimakers.dashboard.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.dashboard.application.port.in.GetDashboardStatsUseCase;
import com.certimakers.dashboard.application.port.in.GetDashboardStatsUseCase.DashboardStatsView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** 관리자 대시보드 통계 API(F-WADM-001). 사용자·진단·상담·룰셋·문서·감사 로그 집계. */
@WebAdapter
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final GetDashboardStatsUseCase getDashboardStatsUseCase;
    private final TimeProvider timeProvider;

    public AdminDashboardController(
            GetDashboardStatsUseCase getDashboardStatsUseCase, TimeProvider timeProvider) {
        this.getDashboardStatsUseCase = getDashboardStatsUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<DashboardStatsView>>> stats() {
        return getDashboardStatsUseCase.get()
                .flatMap(body -> TraceId.current().map(traceId ->
                        ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now()))));
    }
}
