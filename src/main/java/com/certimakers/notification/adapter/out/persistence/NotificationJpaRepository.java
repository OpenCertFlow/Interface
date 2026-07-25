package com.certimakers.notification.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByRecipientUserIdOrderByCreatedAtDesc(
            String recipientUserId, Pageable pageable);

    List<NotificationEntity> findByRecipientUserIdAndReadIsFalseOrderByCreatedAtDesc(
            String recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndReadIsFalse(String recipientUserId);

    Optional<NotificationEntity> findByIdAndRecipientUserId(Long id, String recipientUserId);
}
