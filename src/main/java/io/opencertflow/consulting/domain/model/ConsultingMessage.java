package io.opencertflow.consulting.domain.model;

import io.opencertflow.common.domain.model.Guard;
import java.time.Instant;

/** 상담 스레드의 메시지 한 건. 추가정보 요청·공개 안내·내부 메모를 담는다. */
public record ConsultingMessage(
        Long id, Long leadId, String authorId, MessageKind kind, String body, Instant createdAt) {

    public ConsultingMessage {
        Guard.notNull(id, "id");
        Guard.notNull(leadId, "leadId");
        Guard.notNull(kind, "kind");
        Guard.hasText(body, "body");
        Guard.notNull(createdAt, "createdAt");
    }

    public boolean isPublic() {
        return kind.isPublic();
    }
}
