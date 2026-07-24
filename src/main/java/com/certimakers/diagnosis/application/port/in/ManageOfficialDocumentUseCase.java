package com.certimakers.diagnosis.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** 관리자 공식 문서 메타데이터 관리(F-WADM-012/013). sourceUrl 필수(불변식 6). */
public interface ManageOfficialDocumentUseCase {

    Mono<List<DocumentView>> list();

    Mono<DocumentView> get(UUID id);

    Mono<UUID> register(DocumentCommand command);

    Mono<Void> update(UUID id, DocumentCommand command);

    record DocumentCommand(
            String title, String issuer, LocalDate publishedAt, LocalDate verifiedAt,
            String productGroup, String certificationType, String schemeName, String sourceUrl) {
    }

    record DocumentView(
            UUID id, String title, String issuer, LocalDate publishedAt, LocalDate verifiedAt,
            String productGroup, String certificationType, String schemeName, String sourceUrl,
            Instant createdAt) {
    }
}
