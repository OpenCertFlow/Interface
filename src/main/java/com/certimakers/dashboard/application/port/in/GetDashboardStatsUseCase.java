package com.certimakers.dashboard.application.port.in;

import reactor.core.publisher.Mono;

/** 관리자 대시보드 통계 조회(F-WADM-001). */
public interface GetDashboardStatsUseCase {

    Mono<DashboardStatsView> get();

    record DashboardStatsView(
            long userCount, long consultantCount, long adminCount, long diagnosisCount,
            long consultingLeadCount, long ruleSetCount, long activeRuleSetCount,
            long officialDocumentCount, long auditLogCount) {
    }
}
