package com.certimakers.file.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** {@code stored_file} 테이블 매핑. 파일 바이트가 아니라 메타데이터만 담는다. */
@Entity
@Table(name = "stored_file")
public class StoredFileEntity {

    @Id
    private UUID id;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_in_bytes", nullable = false)
    private long sizeInBytes;

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StoredFileEntity() {
    }

    public StoredFileEntity(
            UUID id, String originalName, String contentType, long sizeInBytes,
            String storageKey, UUID ownerId, Instant createdAt) {
        this.id = id;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeInBytes = sizeInBytes;
        this.storageKey = storageKey;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
