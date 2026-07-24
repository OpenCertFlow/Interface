package com.certimakers.notification.application.port.out;

import java.time.Instant;
import java.util.List;

/** 알림 저장·조회. 블로킹(JPA)이라 호출자는 BlockingBridge로 감싼다. */
public interface NotificationPort {

    void save(String recipientUserId, String kind, String title, String body,
              String refType, String refId, Instant createdAt);

    List<NotificationRow> findByRecipient(String recipientUserId, boolean unreadOnly, int limit);

    long countUnread(String recipientUserId);

    boolean markRead(String recipientUserId, String notificationId);

    record NotificationRow(String id, String kind, String title, String body, String refType,
                           String refId, boolean read, Instant createdAt) {
    }
}
