package com.certimakers.diagnosis.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 공식 문서 메타데이터 조회·등록·수정. 메타데이터만 다루고 색인은 AI워커 소관이다. */
public interface OfficialDocumentAdminPort {

    List<DocumentRow> findAll();

    Optional<DocumentRow> findById(Long id);

    Long register(DocumentData data);

    boolean update(Long id, DocumentData data);

    record DocumentRow(Long id, String title, String issuer, LocalDate publishedAt,
                       LocalDate verifiedAt, String productGroup, String certificationType,
                       String schemeName, String sourceUrl, Instant createdAt) {
    }

    record DocumentData(String title, String issuer, LocalDate publishedAt, LocalDate verifiedAt,
                        String productGroup, String certificationType, String schemeName,
                        String sourceUrl) {
    }
}
