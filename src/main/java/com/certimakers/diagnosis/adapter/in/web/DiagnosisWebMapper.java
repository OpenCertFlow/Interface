package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.CandidateView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ChecklistView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.DegradedView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.EvidenceView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ExpertReviewView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.NarrationView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ScoreView;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ElectricalSpec;
import com.certimakers.diagnosis.domain.model.MaterialType;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.ReadinessScore;
import com.certimakers.diagnosis.domain.model.SalesChannel;
import com.certimakers.diagnosis.domain.model.TargetUser;
import com.certimakers.diagnosis.domain.rule.RuleCode;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 웹 DTO ↔ 도메인 변환. 문자열을 enum으로 옮기는 것이 이 어댑터의 책임이다.
 *
 * <p>잘못된 enum 문자열은 여기서 {@code VALIDATION} 오류로 바뀐다 — 도메인까지 내려가 모호한
 * 예외가 되기 전에 사용자가 고칠 수 있는 메시지를 준다.
 */
@Component
public class DiagnosisWebMapper {

    // ── 요청 → 도메인 ─────────────────────────────────────────────

    public ProductProfile toProfile(DiagnoseRequest request) {
        boolean usesElectricity = request.usesElectricity();
        ElectricalSpec electrical = new ElectricalSpec(
                usesElectricity,
                usesElectricity ? request.ratedVoltage() : null,
                usesElectricity ? request.powerConsumption() : null,
                request.hasBattery());

        return new ProductProfile(
                request.productName(),
                parse(ProductGroup.class, request.productGroup(), "productGroup"),
                electrical,
                parse(TargetUser.class, request.targetUser(), "targetUser"),
                parse(SalesChannel.class, request.salesChannel(), "salesChannel"),
                request.materials().stream()
                        .map(value -> parse(MaterialType.class, value, "materials"))
                        .collect(Collectors.toUnmodifiableSet()),
                request.heldDocuments().stream()
                        .map(DocumentCode::of)
                        .collect(Collectors.toUnmodifiableSet()));
    }

    private <E extends Enum<E>> E parse(Class<E> type, String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.invalid("%s 값이 필요합니다.".formatted(field));
        }
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid(
                    "%s 값이 올바르지 않습니다: %s".formatted(field, raw));
        }
    }

    // ── 도메인 → 응답 ─────────────────────────────────────────────

    public DiagnosisReportResponse toResponse(Diagnosis diagnosis) {
        return new DiagnosisReportResponse(
                diagnosis.id().value().toString(),
                diagnosis.status().name(),
                toScoreView(diagnosis.score()),
                diagnosis.candidates().stream().map(this::toCandidateView).toList(),
                diagnosis.checklist().stream().map(this::toChecklistView).toList(),
                diagnosis.remediationOrder().stream().map(this::toChecklistView).toList(),
                diagnosis.labelingChecks().stream().map(item -> item.label()).toList(),
                diagnosis.expertReviewItems().stream()
                        .map(item -> new ExpertReviewView(item.question(), item.reason().name()))
                        .toList(),
                diagnosis.evidences().stream()
                        .map(evidence -> new EvidenceView(
                                evidence.sectionType(), evidence.snippet(),
                                evidence.sourceUrl().toString(), evidence.relevance()))
                        .toList(),
                diagnosis.narration().map(this::toNarrationView).orElse(null),
                new DegradedView(
                        diagnosis.degraded().isEvidenceDegraded(),
                        diagnosis.degraded().isNarrationDegraded()));
    }

    private ScoreView toScoreView(ReadinessScore score) {
        if (score == null) {
            return new ScoreView(false, 0, 0, 0);
        }
        return new ScoreView(
                score.applicable(), score.percentage(), score.earnedWeight(), score.totalWeight());
    }

    private CandidateView toCandidateView(
            com.certimakers.diagnosis.domain.model.CertificationCandidate candidate) {
        List<String> rules = candidate.matchedRules().stream()
                .map(RuleCode::value).sorted().toList();
        return new CandidateView(candidate.schemeCode().value(), candidate.type().name(), rules);
    }

    private ChecklistView toChecklistView(
            com.certimakers.diagnosis.domain.model.ChecklistItem item) {
        return new ChecklistView(
                item.documentCode().value(), item.requirement().name(), item.weight(), item.held());
    }

    private NarrationView toNarrationView(com.certimakers.diagnosis.domain.model.Narration narration) {
        return new NarrationView(
                narration.summary(), narration.nextActions(), narration.preConsultQuestions(),
                narration.disclaimer(), narration.isTemplateFallback());
    }
}
