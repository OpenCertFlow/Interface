package com.certimakers.consulting.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consulting_message")
public class ConsultingMessageEntity {

    @Id
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "author_id")
    private String authorId;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConsultingMessageEntity() {
    }

    public ConsultingMessageEntity(
            UUID id, UUID leadId, String authorId, String kind, String body, Instant createdAt) {
        this.id = id;
        this.leadId = leadId;
        this.authorId = authorId;
        this.kind = kind;
        this.body = body;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeadId() {
        return leadId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getKind() {
        return kind;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
