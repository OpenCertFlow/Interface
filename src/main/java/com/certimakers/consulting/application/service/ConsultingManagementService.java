package com.certimakers.consulting.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.consulting.application.port.in.ManageConsultingUseCase;
import com.certimakers.consulting.application.port.out.LoadConsultingLeadsPort;
import com.certimakers.consulting.application.port.out.UpdateConsultingLeadPort;
import com.certimakers.consulting.domain.error.ConsultingErrorCode;
import com.certimakers.consulting.domain.model.ConsultingLead;
import com.certimakers.consulting.domain.model.ConsultingLeadId;
import com.certimakers.consulting.domain.model.LeadStatus;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/** 컨설턴트 상담 처리 오케스트레이션. 조회·배정·상태 전이·메모를 BlockingBridge로 격리한다. */
@UseCase
public class ConsultingManagementService implements ManageConsultingUseCase {

    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_LIMIT = 50;

    private final LoadConsultingLeadsPort loadPort;
    private final UpdateConsultingLeadPort updatePort;
    private final BlockingBridge blockingBridge;

    public ConsultingManagementService(
            LoadConsultingLeadsPort loadPort, UpdateConsultingLeadPort updatePort,
            BlockingBridge blockingBridge) {
        this.loadPort = loadPort;
        this.updatePort = updatePort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<List<LeadSummary>> list(String statusFilter, int limit) {
        return Mono.fromSupplier(() -> normalizeStatus(statusFilter))
                .flatMap(status -> {
                    int capped = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
                    return blockingBridge.mono(() -> loadPort.findLeads(status, capped).stream()
                            .map(this::toSummary)
                            .toList());
                });
    }

    @Override
    public Mono<LeadDetail> get(String leadId) {
        ConsultingLeadId id = parseId(leadId);
        return blockingBridge.mono(() -> loadPort.findLead(id).orElse(null))
                .switchIfEmpty(Mono.error(new BusinessException(ConsultingErrorCode.LEAD_NOT_FOUND)))
                .map(this::toDetail);
    }

    @Override
    public Mono<LeadDetail> assign(String leadId, String consultantId) {
        return mutate(leadId, lead -> lead.assignTo(consultantId));
    }

    @Override
    public Mono<LeadDetail> changeStatus(String leadId, String status) {
        LeadStatus target = parseStatus(status);
        return mutate(leadId, lead -> lead.transitionTo(target));
    }

    @Override
    public Mono<LeadDetail> updateMemo(String leadId, String memo) {
        return mutate(leadId, lead -> lead.updateInternalMemo(memo));
    }

    private Mono<LeadDetail> mutate(String leadId, Consumer<ConsultingLead> change) {
        ConsultingLeadId id = parseId(leadId);
        return blockingBridge.mono(() -> {
            ConsultingLead lead = loadPort.findLead(id)
                    .orElseThrow(() -> new BusinessException(ConsultingErrorCode.LEAD_NOT_FOUND));
            change.accept(lead);
            updatePort.update(lead);
            return lead;
        }).map(this::toDetail);
    }

    private LeadStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.invalid("상태 값이 필요합니다.");
        }
        try {
            return LeadStatus.valueOf(raw.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("상태 값이 올바르지 않습니다: " + raw);
        }
    }

    private String normalizeStatus(String raw) {
        return raw == null || raw.isBlank() ? null : parseStatus(raw).name();
    }

    private ConsultingLeadId parseId(String raw) {
        try {
            return ConsultingLeadId.of(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ConsultingErrorCode.LEAD_NOT_FOUND);
        }
    }

    private LeadSummary toSummary(ConsultingLead lead) {
        return new LeadSummary(
                lead.id().value().toString(), lead.diagnosis().value().toString(),
                lead.contact().name(), lead.status().name(),
                lead.assignedConsultantId().orElse(null), lead.createdAt());
    }

    private LeadDetail toDetail(ConsultingLead lead) {
        return new LeadDetail(
                lead.id().value().toString(), lead.diagnosis().value().toString(),
                lead.contact().name(), lead.contact().phone(),
                lead.contact().hasEmail() ? lead.contact().email() : null,
                lead.message().orElse(null), lead.status().name(),
                lead.assignedConsultantId().orElse(null), lead.internalMemo().orElse(null),
                lead.createdAt());
    }
}
