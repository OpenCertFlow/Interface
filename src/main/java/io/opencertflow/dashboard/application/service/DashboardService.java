package io.opencertflow.dashboard.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.dashboard.application.port.in.GetDashboardStatsUseCase;
import io.opencertflow.dashboard.application.port.out.DashboardStatsPort;
import io.opencertflow.dashboard.application.port.out.DashboardStatsPort.Stats;
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
