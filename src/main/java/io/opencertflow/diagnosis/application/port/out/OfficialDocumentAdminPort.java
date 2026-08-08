package io.opencertflow.diagnosis.application.port.out;

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

    /** 원문 확인 결과를 반영한다. 해시가 이전과 다르면 변경 감지 시각이 남는다. */
    void recordContentCheck(Long id, String contentHash, Instant checkedAt);

    /** 변경이 감지되어 재검토가 필요한 문서. 관리자 큐다. */
    List<DocumentRow> findChangeDetected();

    /** 재검토 완료 표시. 변경 플래그를 지운다. */
    boolean clearChangeFlag(Long id);

    record DocumentRow(Long id, String title, String issuer, LocalDate publishedAt,
                       LocalDate verifiedAt, String productGroup, String certificationType,
                       String schemeName, String sourceUrl, Instant createdAt,
                       Instant contentCheckedAt, Instant changeDetectedAt) {
    }

    record DocumentData(String title, String issuer, LocalDate publishedAt, LocalDate verifiedAt,
                        String productGroup, String certificationType, String schemeName,
                        String sourceUrl) {
    }
}
