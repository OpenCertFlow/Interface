package io.opencertflow.notification.application.port.in;

import reactor.core.publisher.Mono;

/** 알림 발행(F-BE-013). 다른 컨텍스트가 이벤트 발생 시 호출한다. 실패해도 원 작업을 깨뜨리지 않는다. */
public interface RecordNotificationUseCase {

    Mono<Void> record(RecordCommand command);

    record RecordCommand(String recipientUserId, String kind, String title, String body,
                         String refType, String refId) {
    }
}
