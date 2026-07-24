package com.certimakers.notification.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class NotificationEntity {

    @Id
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false)
    private String recipientUserId;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String title;

    @Column
    private String body;

    @Column(name = "ref_type")
    private String refType;

    @Column(name = "ref_id")
    private String refId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationEntity() {
    }

    public NotificationEntity(
            UUID id, String recipientUserId, String kind, String title, String body,
            String refType, String refId, Instant createdAt) {
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.refType = refType;
        this.refId = refId;
        this.read = false;
        this.createdAt = createdAt;
    }

    public void markRead() {
        this.read = true;
    }

    public UUID getId() {
        return id;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public String getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getRefType() {
        return refType;
    }

    public String getRefId() {
        return refId;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
