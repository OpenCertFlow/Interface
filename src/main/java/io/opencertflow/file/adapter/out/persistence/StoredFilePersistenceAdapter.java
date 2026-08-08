package io.opencertflow.file.adapter.out.persistence;

import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.file.application.port.out.DeleteFilePort;
import io.opencertflow.file.application.port.out.LoadFilePort;
import io.opencertflow.file.application.port.out.SaveFilePort;
import io.opencertflow.file.domain.model.ContentType;
import io.opencertflow.file.domain.model.FileId;
import io.opencertflow.file.domain.model.OriginalFileName;
import io.opencertflow.file.domain.model.OwnerRef;
import io.opencertflow.file.domain.model.StorageKey;
import io.opencertflow.file.domain.model.StoredFile;
import io.opencertflow.file.domain.model.Visibility;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/** 파일 메타데이터 포트의 JPA 구현. 메서드는 블로킹이다. */
@PersistenceAdapter
public class StoredFilePersistenceAdapter implements SaveFilePort, LoadFilePort, DeleteFilePort {

    private final StoredFileJpaRepository repository;

    public StoredFilePersistenceAdapter(StoredFileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public StoredFile save(StoredFile file) {
        repository.save(toEntity(file));
        return file;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findById(FileId id) {
        return repository.findById(id.value()).map(StoredFilePersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredFile> findAllByIds(Collection<FileId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Long> raw = ids.stream().map(FileId::value).toList();
        return repository.findAllByIdIn(raw).stream()
                .map(StoredFilePersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(FileId id) {
        repository.deleteById(id.value());
    }

    private static StoredFileEntity toEntity(StoredFile file) {
        return new StoredFileEntity(
                file.id().value(),
                file.originalName().value(),
                file.contentType().value(),
                file.sizeInBytes(),
                file.storageKey().value(),
                file.owner().value(),
                file.visibility().name(),
                file.createdAt());
    }

    private static StoredFile toDomain(StoredFileEntity entity) {
        return StoredFile.reconstitute(
                FileId.of(entity.getId()),
                OriginalFileName.of(entity.getOriginalName()),
                ContentType.of(entity.getContentType()),
                entity.getSizeInBytes(),
                StorageKey.of(entity.getStorageKey()),
                OwnerRef.of(entity.getOwnerId()),
                Visibility.valueOf(entity.getVisibility()),
                entity.getCreatedAt());
    }
}
