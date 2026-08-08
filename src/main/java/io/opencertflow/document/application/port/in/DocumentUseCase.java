package io.opencertflow.document.application.port.in;

import io.opencertflow.document.domain.model.DocumentTemplate;
import io.opencertflow.document.domain.model.IssuedDocument;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

/** 문서 양식 조회와 발급. */
public interface DocumentUseCase {

    /** 발급 가능한 양식과 입력 항목. 클라이언트가 입력 화면을 서버 정의대로 그리게 한다. */
    List<DocumentTemplate> templates();

    /** 양식에 값을 채워 PDF를 만들고 저장한다. */
    Mono<IssuedResult> issue(IssueCommand command);

    /** 내가 발급한 문서 목록. */
    Mono<List<IssuedDocument>> myDocuments(String requesterId, int page, int size);

    /** 발급 문서 한 건. 발급자 본인 또는 관리자만 볼 수 있다. */
    Mono<IssuedDocument> get(String documentId, String requesterId, boolean requesterIsAdmin);

    /**
     * @param templateCode 양식 코드
     * @param values       항목 코드 → 값
     * @param issuerId     발급자 식별자
     */
    record IssueCommand(String templateCode, Map<String, String> values, String issuerId) {
    }

    /**
     * @param document    발급 이력
     * @param downloadUrl 생성된 PDF 다운로드 경로
     */
    record IssuedResult(IssuedDocument document, String downloadUrl) {
    }
}
