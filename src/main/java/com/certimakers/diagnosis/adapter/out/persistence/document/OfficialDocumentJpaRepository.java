package com.certimakers.diagnosis.adapter.out.persistence.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialDocumentJpaRepository extends JpaRepository<OfficialDocumentEntity, Long> {

    List<OfficialDocumentEntity> findAllByOrderByCreatedAtDesc();
}
