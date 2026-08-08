package io.opencertflow.consulting.application.service;

import io.opencertflow.common.application.annotation.UseCase;
import io.opencertflow.common.application.support.BlockingBridge;
import io.opencertflow.consulting.application.port.in.GetMyLeadsUseCase;
import io.opencertflow.consulting.application.port.out.LoadConsultingLeadsPort;
import io.opencertflow.consulting.domain.model.ConsultingLead;
import java.util.List;
import reactor.core.publisher.Mono;

@UseCase
public class MyConsultingService implements GetMyLeadsUseCase {

    private static final int MAX_LIMIT = 100;

    private final LoadConsultingLeadsPort loadPort;
    private final BlockingBridge blockingBridge;

    public MyConsultingService(LoadConsultingLeadsPort loadPort, BlockingBridge blockingBridge) {
        this.loadPort = loadPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<List<MyLeadView>> myLeads(String ownerUserId, int limit) {
        int capped = limit <= 0 ? 50 : Math.min(limit, MAX_LIMIT);
        return blockingBridge.mono(() -> loadPort.findByOwner(ownerUserId, capped).stream()
                .map(this::toView)
                .toList());
    }

    private MyLeadView toView(ConsultingLead lead) {
        return new MyLeadView(
                lead.id().value().toString(), lead.diagnosis().value().toString(),
                lead.status().name(), lead.createdAt());
    }
}
