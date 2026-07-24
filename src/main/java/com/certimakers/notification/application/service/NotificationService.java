package com.certimakers.notification.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.notification.application.port.in.QueryNotificationUseCase;
import com.certimakers.notification.application.port.in.RecordNotificationUseCase;
import com.certimakers.notification.application.port.out.NotificationPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/** 알림 발행·조회. 발행 실패는 로그만 남기고 삼킨다 — 알림이 원 작업(상담 처리 등)을 막지 않는다. */
@UseCase
public class NotificationService implements RecordNotificationUseCase, QueryNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 30;

    private final NotificationPort notificationPort;
    private final BlockingBridge blockingBridge;
    private final TimeProvider timeProvider;

    public NotificationService(
            NotificationPort notificationPort, BlockingBridge blockingBridge,
            TimeProvider timeProvider) {
        this.notificationPort = notificationPort;
        this.blockingBridge = blockingBridge;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<Void> record(RecordCommand command) {
        if (command.recipientUserId() == null || command.recipientUserId().isBlank()) {
            return Mono.empty(); // 수신자가 없으면(익명 리드) 알림을 만들지 않는다
        }
        return blockingBridge.run(() -> notificationPort.save(
                        command.recipientUserId(), command.kind(), command.title(), command.body(),
                        command.refType(), command.refId(), timeProvider.now()))
                .onErrorResume(error -> {
                    log.warn("알림 발행 실패 — 원 작업은 계속한다. kind={}, cause={}",
                            command.kind(), error.toString());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<List<NotificationView>> myNotifications(String userId, boolean unreadOnly, int limit) {
        int capped = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return blockingBridge.mono(() ->
                notificationPort.findByRecipient(userId, unreadOnly, capped).stream()
                        .map(row -> new NotificationView(
                                row.id(), row.kind(), row.title(), row.body(),
                                row.refType(), row.refId(), row.read(), row.createdAt()))
                        .toList());
    }

    @Override
    public Mono<Long> unreadCount(String userId) {
        return blockingBridge.mono(() -> notificationPort.countUnread(userId));
    }

    @Override
    public Mono<Boolean> markRead(String userId, String notificationId) {
        return blockingBridge.mono(() -> notificationPort.markRead(userId, notificationId));
    }
}
