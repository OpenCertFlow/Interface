package io.opencertflow.diagnosis.application.port.in;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 컨설턴트가 근거의 적절성을 되먹인다.
 *
 * <p>지금까지 근거 품질은 일방향이었다 — 관리자가 색인하고 사용자가 본다. 그런데 그 근거가 이
 * 제품에 맞는지 가장 잘 아는 사람은 상담을 처리하는 컨설턴트다. 그 판단을 받아 두면 색인 재검토의
 * 우선순위가 생기고, 양면 구조가 데이터로 순환한다.
 */
public interface EvidenceFeedbackUseCase {

    /** 근거 하나에 대한 판단을 남긴다. */
    Mono<Void> report(ReportCommand command);

    /** 문서별 집계. 재검토가 필요한 순서로 돌려준다. */
    Mono<List<DocumentFeedbackSummary>> summary();

    record ReportCommand(
            Long diagnosisId, String sourceDocumentId, String sectionType,
            String verdict, String comment, String reportedBy) {
    }

    /**
     * @param needsReviewCount USEFUL이 아닌 판단의 수. 이 값이 큰 문서부터 손봐야 한다
     */
    record DocumentFeedbackSummary(
            String sourceDocumentId, long total, long usefulCount, long needsReviewCount,
            Instant lastReportedAt) {
    }
}
