package io.opencertflow.diagnosis.application.port.in;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 공식 문서 원문이 바뀌었는지 살핀다.
 *
 * <p>운영지침은 "공식 자료를 자동으로 항상 최신 상태로 갱신한다고 표현하지 않는다"로 못을 박았다.
 * 이 유스케이스는 그 원칙을 어기지 않는다 — <b>갱신하지 않고 감지만 한다.</b> 무엇을 어떻게 고칠지는
 * 사람이 원문을 읽고 정한다.
 */
public interface MonitorDocumentFreshnessUseCase {

    /** 등록된 문서의 원문을 훑어 해시를 견준다. 실행 요약을 돌려준다. */
    Mono<CheckSummary> checkAll();

    /** 변경이 감지되어 재검토가 필요한 문서 목록. */
    Mono<List<StaleDocumentView>> pendingReview();

    /** 재검토를 마쳤다고 표시한다. */
    Mono<Void> markReviewed(Long documentId);

    /**
     * @param checked   원문을 확인해 본 문서 수
     * @param changed   이번 실행에서 변경이 감지된 수
     * @param unreachable 원문을 가져오지 못한 수. '변경됨'과 구별해 센다
     */
    record CheckSummary(int checked, int changed, int unreachable) {
    }

    record StaleDocumentView(
            Long id, String title, String issuer, String sourceUrl,
            Instant contentCheckedAt, Instant changeDetectedAt) {
    }
}
