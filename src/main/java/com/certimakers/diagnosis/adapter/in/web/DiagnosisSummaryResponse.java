package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.diagnosis.domain.model.DiagnosisSummary;
import java.time.Instant;

/** 진단 이력 목록 응답의 한 줄(F-APP-032). */
public record DiagnosisSummaryResponse(
        String id,
        String productName,
        String productGroup,
        String status,
        Integer readinessScore,
        boolean scoreApplicable,
        Instant createdAt,
        String previousDiagnosisId) {

    public static DiagnosisSummaryResponse from(DiagnosisSummary summary) {
        return new DiagnosisSummaryResponse(
                Long.toString(summary.id()),
                summary.productName(),
                summary.productGroup().name(),
                summary.status().name(),
                summary.readinessScore(),
                summary.scoreApplicable(),
                summary.createdAt(),
                summary.previousDiagnosisId() == null
                        ? null : Long.toString(summary.previousDiagnosisId()));
    }
}
