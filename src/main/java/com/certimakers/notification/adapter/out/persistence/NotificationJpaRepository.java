package com.certimakers.notification.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByRecipientUserIdOrderByCreatedAtDesc(
            String recipientUserId, Pageable pageable);

    List<NotificationEntity> findByRecipientUserIdAndReadIsFalseOrderByCreatedAtDesc(
            String recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndReadIsFalse(String recipientUserId);

    Optional<NotificationEntity> findByIdAndRecipientUserId(UUID id, String recipientUserId);
}
