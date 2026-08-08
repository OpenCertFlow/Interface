package io.opencertflow.file.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code stored_file} 스프링 데이터 리포지토리. */
public interface StoredFileJpaRepository extends JpaRepository<StoredFileEntity, Long> {

    List<StoredFileEntity> findAllByIdIn(Collection<Long> ids);
}
