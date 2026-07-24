package com.certimakers.diagnosis.adapter.out.persistence.document;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialDocumentJpaRepository extends JpaRepository<OfficialDocumentEntity, UUID> {

    List<OfficialDocumentEntity> findAllByOrderByCreatedAtDesc();
}
