package io.opencertflow.diagnosis.adapter.out.persistence.feedback;

import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.common.domain.port.IdGenerator;
import io.opencertflow.diagnosis.application.port.out.EvidenceFeedbackPort;
import io.opencertflow.diagnosis.domain.model.EvidenceVerdict;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/**
 * 근거 피드백 저장·집계.
 *
 * <p>집계를 SQL이 아니라 메모리에서 한다. 피드백은 상담 건수에 비례해 쌓이므로 규모가 작고,
 * 판단 종류가 늘어날 때 쿼리를 고치는 것보다 도메인 열거형 하나만 보는 편이 안전하다.
 * 규모가 커지면 그때 집계 쿼리로 옮긴다.
 */
@PersistenceAdapter
public class EvidenceFeedbackPersistenceAdapter implements EvidenceFeedbackPort {

    private final EvidenceFeedbackJpaRepository repository;
    private final IdGenerator idGenerator;

    public EvidenceFeedbackPersistenceAdapter(
            EvidenceFeedbackJpaRepository repository, IdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public void save(FeedbackData data) {
        repository.save(new EvidenceFeedbackEntity(
                idGenerator.nextId(), data.diagnosisId(), data.sourceDocumentId(),
                data.sectionType(), data.verdict(), data.comment(), data.reportedBy()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummaryRow> summarize() {
        Map<String, Accumulator> byDocument = new LinkedHashMap<>();
        for (EvidenceFeedbackEntity entity : repository.findAllByOrderByCreatedAtDesc()) {
            byDocument.computeIfAbsent(entity.getSourceDocumentId(), key -> new Accumulator())
                    .add(entity);
        }
        List<DocumentSummaryRow> rows = new ArrayList<>();
        byDocument.forEach((documentId, acc) -> rows.add(new DocumentSummaryRow(
                documentId, acc.total, acc.useful, acc.needsReview, acc.lastReportedAt)));

        // 재검토가 필요한 건수가 많은 문서부터. 관리자가 위에서부터 손보면 된다.
        rows.sort(Comparator.comparingLong(DocumentSummaryRow::needsReviewCount).reversed()
                .thenComparing(DocumentSummaryRow::sourceDocumentId));
        return rows;
    }

    private static final class Accumulator {
        private long total;
        private long useful;
        private long needsReview;
        private Instant lastReportedAt;

        void add(EvidenceFeedbackEntity entity) {
            total++;
            if (isUseful(entity.getVerdict())) {
                useful++;
            } else {
                needsReview++;
            }
            if (lastReportedAt == null
                    || (entity.getCreatedAt() != null && entity.getCreatedAt().isAfter(lastReportedAt))) {
                lastReportedAt = entity.getCreatedAt();
            }
        }

        /** 알 수 없는 값은 재검토 대상으로 본다 — 모르는 것을 좋다고 세지 않는다. */
        private boolean isUseful(String verdict) {
            try {
                return !EvidenceVerdict.valueOf(verdict).needsReview();
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}
