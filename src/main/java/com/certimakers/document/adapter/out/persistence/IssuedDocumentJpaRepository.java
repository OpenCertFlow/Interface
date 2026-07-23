package com.certimakers.document.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code issued_document} 스프링 데이터 리포지토리. */
public interface IssuedDocumentJpaRepository extends JpaRepository<IssuedDocumentEntity, UUID> {

    List<IssuedDocumentEntity> findByIssuerIdOrderByIssuedAtDesc(UUID issuerId, Pageable pageable);
}
