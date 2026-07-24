package com.certimakers.consulting.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.consulting.application.port.in.GetMyLeadsUseCase;
import com.certimakers.consulting.application.port.out.LoadConsultingLeadsPort;
import com.certimakers.consulting.domain.model.ConsultingLead;
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
