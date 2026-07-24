package com.certimakers.dashboard.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.dashboard.application.port.in.GetDashboardStatsUseCase;
import com.certimakers.dashboard.application.port.out.DashboardStatsPort;
import com.certimakers.dashboard.application.port.out.DashboardStatsPort.Stats;
import reactor.core.publisher.Mono;

@UseCase
public class DashboardService implements GetDashboardStatsUseCase {

    private final DashboardStatsPort statsPort;
    private final BlockingBridge blockingBridge;

    public DashboardService(DashboardStatsPort statsPort, BlockingBridge blockingBridge) {
        this.statsPort = statsPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<DashboardStatsView> get() {
        return blockingBridge.mono(statsPort::load).map(this::toView);
    }

    private DashboardStatsView toView(Stats s) {
        return new DashboardStatsView(
                s.userCount(), s.consultantCount(), s.adminCount(), s.diagnosisCount(),
                s.consultingLeadCount(), s.ruleSetCount(), s.activeRuleSetCount(),
                s.officialDocumentCount(), s.auditLogCount());
    }
}
