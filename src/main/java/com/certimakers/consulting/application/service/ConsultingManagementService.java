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
import com.certimakers.notification.application.port.in.RecordNotificationUseCase;
import com.certimakers.notification.application.port.in.RecordNotificationUseCase.RecordCommand;
import java.util.List;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/** 컨설턴트 상담 처리 오케스트레이션. 조회·배정·상태 전이·메모를 BlockingBridge로 격리한다. */
@UseCase
public class ConsultingManagementService implements ManageConsultingUseCase {

    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_LIMIT = 50;

    private final LoadConsultingLeadsPort loadPort;
    private final UpdateConsultingLeadPort updatePort;
    private final RecordNotificationUseCase notificationUseCase;
    private final BlockingBridge blockingBridge;

    public ConsultingManagementService(
            LoadConsultingLeadsPort loadPort, UpdateConsultingLeadPort updatePort,
            RecordNotificationUseCase notificationUseCase, BlockingBridge blockingBridge) {
        this.loadPort = loadPort;
        this.updatePort = updatePort;
        this.notificationUseCase = notificationUseCase;
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
        return mutate(leadId, lead -> lead.assignTo(consultantId))
                .flatMap(lead -> notifyOwner(lead, "CONSULTING_ASSIGNED",
                        "상담 담당이 배정되었습니다",
                        "담당 컨설턴트가 배정되어 곧 연락드릴 예정입니다.")
                        .thenReturn(toDetail(lead)));
    }

    @Override
    public Mono<LeadDetail> changeStatus(String leadId, String status) {
        LeadStatus target = parseStatus(status);
        return mutate(leadId, lead -> lead.transitionTo(target))
                .flatMap(lead -> notifyOwner(lead, "CONSULTING_STATUS",
                        "상담 상태가 변경되었습니다",
                        "상담 상태가 '%s'(으)로 변경되었습니다.".formatted(lead.status().name()))
                        .thenReturn(toDetail(lead)));
    }

    @Override
    public Mono<LeadDetail> updateMemo(String leadId, String memo) {
        return mutate(leadId, lead -> lead.updateInternalMemo(memo)).map(this::toDetail);
    }

    private Mono<ConsultingLead> mutate(String leadId, Consumer<ConsultingLead> change) {
        ConsultingLeadId id = parseId(leadId);
        return blockingBridge.mono(() -> {
            ConsultingLead lead = loadPort.findLead(id)
                    .orElseThrow(() -> new BusinessException(ConsultingErrorCode.LEAD_NOT_FOUND));
            change.accept(lead);
            updatePort.update(lead);
            return lead;
        });
    }

    /** 소유자(로그인 접수자)에게 알림을 보낸다. 익명 리드면 아무 일도 하지 않는다. 실패는 삼킨다. */
    private Mono<Void> notifyOwner(ConsultingLead lead, String kind, String title, String body) {
        return lead.ownerUserId()
                .map(owner -> notificationUseCase.record(new RecordCommand(
                        owner, kind, title, body, "CONSULTING_LEAD", lead.id().value().toString())))
                .orElseGet(Mono::empty);
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
            return ConsultingLeadId.of(Long.parseLong(raw));
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
