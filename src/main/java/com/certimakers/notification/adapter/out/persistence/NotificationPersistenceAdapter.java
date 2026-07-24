package com.certimakers.notification.adapter.out.persistence;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.notification.application.port.out.NotificationPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
public class NotificationPersistenceAdapter implements NotificationPort {

    private final NotificationJpaRepository repository;
    private final IdGenerator idGenerator;

    public NotificationPersistenceAdapter(
            NotificationJpaRepository repository, IdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public void save(String recipientUserId, String kind, String title, String body,
                     String refType, String refId, Instant createdAt) {
        repository.save(new NotificationEntity(
                idGenerator.nextId(), recipientUserId, kind, title, body, refType, refId, createdAt));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRow> findByRecipient(String recipientUserId, boolean unreadOnly, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        List<NotificationEntity> entities = unreadOnly
                ? repository.findByRecipientUserIdAndReadIsFalseOrderByCreatedAtDesc(recipientUserId, page)
                : repository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId, page);
        return entities.stream()
                .map(e -> new NotificationRow(
                        e.getId().toString(), e.getKind(), e.getTitle(), e.getBody(),
                        e.getRefType(), e.getRefId(), e.isRead(), e.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String recipientUserId) {
        return repository.countByRecipientUserIdAndReadIsFalse(recipientUserId);
    }

    @Override
    @Transactional
    public boolean markRead(String recipientUserId, String notificationId) {
        UUID id;
        try {
            id = UUID.fromString(notificationId);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return repository.findByIdAndRecipientUserId(id, recipientUserId)
                .map(entity -> {
                    entity.markRead();
                    repository.save(entity);
                    return true;
                })
                .orElse(false);
    }
}
