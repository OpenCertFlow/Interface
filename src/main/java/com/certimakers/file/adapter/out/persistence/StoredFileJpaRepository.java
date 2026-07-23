package com.certimakers.file.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code stored_file} 스프링 데이터 리포지토리. */
public interface StoredFileJpaRepository extends JpaRepository<StoredFileEntity, UUID> {

    List<StoredFileEntity> findAllByIdIn(Collection<UUID> ids);
}
