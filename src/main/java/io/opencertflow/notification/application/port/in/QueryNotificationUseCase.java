package io.opencertflow.notification.application.port.in;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/** 내 알림 조회·읽음 처리(F-APP-045). */
public interface QueryNotificationUseCase {

    Mono<List<NotificationView>> myNotifications(String userId, boolean unreadOnly, int limit);

    Mono<Long> unreadCount(String userId);

    Mono<Boolean> markRead(String userId, String notificationId);

    record NotificationView(String id, String kind, String title, String body, String refType,
                            String refId, boolean read, Instant createdAt) {
    }
}
