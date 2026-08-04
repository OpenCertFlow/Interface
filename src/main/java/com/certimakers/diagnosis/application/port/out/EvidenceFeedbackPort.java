package com.certimakers.diagnosis.application.port.out;

import java.time.Instant;
import java.util.List;

/** 근거 피드백 저장·집계. */
public interface EvidenceFeedbackPort {

    void save(FeedbackData data);

    /** 문서별 집계. 재검토 필요 건수가 많은 순. */
    List<DocumentSummaryRow> summarize();

    record FeedbackData(
            Long diagnosisId, String sourceDocumentId, String sectionType,
            String verdict, String comment, String reportedBy) {
    }

    record DocumentSummaryRow(
            String sourceDocumentId, long total, long usefulCount, long needsReviewCount,
            Instant lastReportedAt) {
    }
}
