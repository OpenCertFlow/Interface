package com.certimakers.file.domain.model;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.model.AggregateRoot;
import com.certimakers.common.domain.model.Guard;
import com.certimakers.file.domain.error.FileErrorCode;
import java.time.Instant;

/**
 * 저장된 파일의 메타데이터 애그리거트. <b>파일 내용 자체는 담지 않는다.</b>
 *
 * <p>바이트는 저장소(로컬 디스크·오브젝트 스토리지)에 있고, 이 애그리거트는 "어떤 파일이 어디에
 * 있고 누가 올렸는가"만 안다. 내용을 애그리거트에 담으면 메타데이터 한 줄을 읽을 때마다 파일 전체를
 * 메모리에 올리게 된다.
 */
public class StoredFile extends AggregateRoot<FileId> {

    private final FileId id;
    private final OriginalFileName originalName;
    private final ContentType contentType;
    private final long sizeInBytes;
    private final StorageKey storageKey;
    private final OwnerRef owner;
    private final Instant createdAt;

    private StoredFile(
            FileId id, OriginalFileName originalName, ContentType contentType, long sizeInBytes,
            StorageKey storageKey, OwnerRef owner, Instant createdAt) {
        this.id = Guard.notNull(id, "id");
        this.originalName = Guard.notNull(originalName, "originalName");
        this.contentType = Guard.notNull(contentType, "contentType");
        this.storageKey = Guard.notNull(storageKey, "storageKey");
        this.owner = Guard.notNull(owner, "owner");
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        if (sizeInBytes <= 0) {
            throw new BusinessException(FileErrorCode.EMPTY_FILE);
        }
        this.sizeInBytes = sizeInBytes;
    }

    public static StoredFile register(
            FileId id, OriginalFileName originalName, ContentType contentType, long sizeInBytes,
            StorageKey storageKey, OwnerRef owner, Instant createdAt) {
        return new StoredFile(id, originalName, contentType, sizeInBytes, storageKey, owner, createdAt);
    }

    /** 저장된 상태에서 되살린다(영속성 재구성 전용). */
    public static StoredFile reconstitute(
            FileId id, OriginalFileName originalName, ContentType contentType, long sizeInBytes,
            StorageKey storageKey, OwnerRef owner, Instant createdAt) {
        return new StoredFile(id, originalName, contentType, sizeInBytes, storageKey, owner, createdAt);
    }

    /**
     * 삭제 권한을 확인한다. 업로더 본인 또는 관리자만 지울 수 있다.
     *
     * <p>권한 판단을 애그리거트에 두는 이유는, 이 규칙이 새는 순간 남의 파일을 지울 수 있게 되기
     * 때문이다. 컨트롤러마다 조건문을 흩어 두면 한 곳만 빠뜨려도 사고가 난다.
     */
    public void requireDeletableBy(OwnerRef requester, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return;
        }
        if (!owner.equals(requester)) {
            throw new BusinessException(FileErrorCode.NOT_FILE_OWNER);
        }
    }

    public boolean isOwnedBy(OwnerRef candidate) {
        return owner.equals(candidate);
    }

    @Override
    public FileId id() {
        return id;
    }

    public OriginalFileName originalName() {
        return originalName;
    }

    public ContentType contentType() {
        return contentType;
    }

    public long sizeInBytes() {
        return sizeInBytes;
    }

    public StorageKey storageKey() {
        return storageKey;
    }

    public OwnerRef owner() {
        return owner;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
