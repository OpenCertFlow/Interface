package com.certimakers.consulting.application.port.in;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 상담 메시지(F-WCON-008 추가정보 요청, F-WCON-009 공개 안내, F-WCON-011 이력).
 *
 * <p>컨설턴트가 메시지를 남기고, 전체 스레드(컨설턴트)와 공개 메시지(소공인)를 조회한다.
 * 공개 조회는 INFO_REQUEST·REPLY만 보이고 내부 메모(NOTE)와 작성자는 제외한다.
 */
public interface ConsultingMessageUseCase {

    Mono<Void> post(String leadId, String authorId, String kind, String body);

    Mono<List<MessageView>> list(String leadId);

    Mono<List<MessageView>> listPublic(String leadId);

    record MessageView(String kind, String body, Instant createdAt) {
    }
}
