package com.certimakers.document.application.port.out;

import com.certimakers.document.domain.model.DocumentId;
import com.certimakers.document.domain.model.IssuedDocument;
import com.certimakers.document.domain.model.IssuerRef;
import java.util.List;
import java.util.Optional;

/** 발급 이력 저장·조회. 블로킹이므로 호출자는 BlockingBridge로 감싼다. */
public interface IssuedDocumentRepositoryPort {

    IssuedDocument save(IssuedDocument document);

    Optional<IssuedDocument> findById(DocumentId id);

    /** 내가 발급한 문서를 최신순으로. */
    List<IssuedDocument> findByIssuer(IssuerRef issuer, int page, int size);
}
