package com.certimakers.consulting.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.consulting.application.port.in.ConsultingMessageUseCase;
import com.certimakers.consulting.application.port.out.ConsultingMessagePort;
import com.certimakers.consulting.application.port.out.LoadConsultingLeadsPort;
import com.certimakers.consulting.domain.error.ConsultingErrorCode;
import com.certimakers.consulting.domain.model.ConsultingLead;
import com.certimakers.consulting.domain.model.ConsultingLeadId;
import com.certimakers.consulting.domain.model.ConsultingMessage;
import com.certimakers.consulting.domain.model.MessageKind;
import com.certimakers.notification.application.port.in.RecordNotificationUseCase;
import com.certimakers.notification.application.port.in.RecordNotificationUseCase.RecordCommand;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** 상담 메시지 오케스트레이션. id·시각 생성과 저장을 BlockingBridge 위에서 수행한다. */
@UseCase
public class ConsultingMessageService implements ConsultingMessageUseCase {

    private final ConsultingMessagePort messagePort;
    private final LoadConsultingLeadsPort loadPort;
    private final RecordNotificationUseCase notificationUseCase;
    private final BlockingBridge blockingBridge;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public ConsultingMessageService(
            ConsultingMessagePort messagePort, LoadConsultingLeadsPort loadPort,
            RecordNotificationUseCase notificationUseCase, BlockingBridge blockingBridge,
            IdGenerator idGenerator, TimeProvider timeProvider) {
        this.messagePort = messagePort;
        this.loadPort = loadPort;
        this.notificationUseCase = notificationUseCase;
        this.blockingBridge = blockingBridge;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<Void> post(String leadId, String authorId, String kind, String body) {
        UUID lead = parseId(leadId);
        MessageKind messageKind = parseKind(kind);
        if (body == null || body.isBlank()) {
            throw BusinessException.invalid("메시지 내용이 필요합니다.");
        }
        return blockingBridge.mono(() -> {
            ConsultingLead target = loadPort.findLead(ConsultingLeadId.of(lead))
                    .orElseThrow(() -> new BusinessException(ConsultingErrorCode.LEAD_NOT_FOUND));
            messagePort.append(new ConsultingMessage(
                    idGenerator.nextId(), lead, authorId, messageKind, body.strip(),
                    timeProvider.now()));
            return target;
        }).flatMap(target -> notifyOwnerIfPublic(target, messageKind, body.strip()));
    }

    /** 공개 메시지(추가정보 요청·안내)면 소유자에게 알린다. 익명 리드·내부 메모는 알리지 않는다. */
    private Mono<Void> notifyOwnerIfPublic(ConsultingLead lead, MessageKind kind, String body) {
        if (!kind.isPublic() || lead.ownerUserId().isEmpty()) {
            return Mono.empty();
        }
        String title = kind == MessageKind.INFO_REQUEST ? "추가 정보 요청이 도착했습니다"
                : "상담 안내가 도착했습니다";
        return notificationUseCase.record(new RecordCommand(
                lead.ownerUserId().get(), "CONSULTING_MESSAGE", title, body,
                "CONSULTING_LEAD", lead.id().value().toString()));
    }

    @Override
    public Mono<List<MessageView>> list(String leadId) {
        UUID lead = parseId(leadId);
        return blockingBridge.mono(() -> messagePort.findByLead(lead).stream()
                .map(this::toView)
                .toList());
    }

    @Override
    public Mono<List<MessageView>> listPublic(String leadId) {
        UUID lead = parseId(leadId);
        return blockingBridge.mono(() -> messagePort.findByLead(lead).stream()
                .filter(ConsultingMessage::isPublic)
                .map(this::toView)
                .toList());
    }

    private MessageView toView(ConsultingMessage message) {
        return new MessageView(message.kind().name(), message.body(), message.createdAt());
    }

    private MessageKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.invalid("메시지 종류가 필요합니다.");
        }
        try {
            return MessageKind.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("메시지 종류가 올바르지 않습니다: " + raw);
        }
    }

    private UUID parseId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ConsultingErrorCode.LEAD_NOT_FOUND);
        }
    }
}
