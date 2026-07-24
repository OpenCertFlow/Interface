package com.certimakers.audit.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String actor;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(UUID id, String actor, String httpMethod, String requestPath,
                          Integer statusCode, Instant occurredAt) {
        this.id = id;
        this.actor = actor;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.statusCode = statusCode;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
