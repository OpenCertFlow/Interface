package io.opencertflow.diagnosis.adapter.in.web;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.diagnosis.adapter.in.web.DiagnosisReportResponse.CandidateView;
import io.opencertflow.diagnosis.adapter.in.web.DiagnosisReportResponse.ChecklistView;
import io.opencertflow.diagnosis.adapter.in.web.DiagnosisReportResponse.ExpertReviewView;
import io.opencertflow.diagnosis.adapter.in.web.DiagnosisReportResponse.ScoreView;
import io.opencertflow.diagnosis.domain.model.CertificationCandidate;
import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.ReadinessScore;
import io.opencertflow.diagnosis.domain.model.SalesChannel;
import io.opencertflow.diagnosis.domain.model.TargetUser;
import io.opencertflow.diagnosis.domain.rule.RuleCode;
import io.opencertflow.diagnosis.domain.simulation.ProfileAdjustment;
import io.opencertflow.diagnosis.domain.simulation.RemediationPlan;
import io.opencertflow.diagnosis.domain.simulation.RemediationStep;
import io.opencertflow.diagnosis.domain.simulation.SimulationOutcome;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 시뮬레이션 웹 DTO ↔ 도메인 변환. */
@Component
public class SimulationWebMapper {

    /**
     * 시뮬레이션 결과는 가정일 뿐 확정된 진단이 아니다. 사용자가 이 숫자를 "인증을 받을 수 있다"로
     * 오해하지 않도록 응답에 고지를 함께 싣는다.
     */
    private static final String SIMULATION_NOTICE =
            "가정에 따른 예상 결과이며 저장되지 않습니다. 준비도는 공식 요구자료 대비 준비 수준이고 "
                    + "인증 합격을 예측하지 않습니다.";

    private static final String PLAN_NOTICE =
            "준비도는 공식 요구자료 대비 준비 수준이며 인증 합격을 예측하지 않습니다. "
                    + "서류를 갖춰도 시험·심사 결과에 따라 결과가 달라질 수 있습니다.";

    // ── 요청 → 도메인 ─────────────────────────────────────────────

    public ProfileAdjustment toAdjustment(SimulateRequest request) {
        return new ProfileAdjustment(
                toDocumentCodes(request.addDocuments()),
                toDocumentCodes(request.removeDocuments()),
                request.usesElectricity(),
                request.ratedVoltage(),
                request.powerConsumption(),
                request.hasBattery(),
                parseOrNull(TargetUser.class, request.targetUser(), "targetUser"),
                parseOrNull(SalesChannel.class, request.salesChannel(), "salesChannel"));
    }

    private java.util.Set<DocumentCode> toDocumentCodes(List<String> raw) {
        return raw.stream().map(DocumentCode::of).collect(Collectors.toUnmodifiableSet());
    }

    /** null·공백은 "변경 없음"이다. 값이 있는데 enum에 없으면 사용자가 고칠 수 있는 오류로 바꾼다. */
    private <E extends Enum<E>> E parseOrNull(Class<E> type, String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("%s 값이 올바르지 않습니다: %s".formatted(field, raw));
        }
    }

    // ── 도메인 → 응답 ─────────────────────────────────────────────

    public SimulationResponse toResponse(String diagnosisId, SimulationOutcome outcome) {
        return new SimulationResponse(
                diagnosisId,
                toScoreView(outcome.scoreDelta().before()),
                toScoreView(outcome.scoreDelta().after()),
                outcome.scoreDelta().comparable(),
                outcome.scoreDelta().percentagePointChange(),
                outcome.certificationScopeChanged(),
                outcome.addedCandidates().stream().map(this::toCandidateView).toList(),
                outcome.removedCandidates().stream().map(this::toCandidateView).toList(),
                toCodes(outcome.newlyRequiredDocuments()),
                toCodes(outcome.noLongerRequiredDocuments()),
                toCodes(outcome.newlySatisfiedDocuments()),
                outcome.scoreResult().checklist().stream().map(this::toChecklistView).toList(),
                outcome.scoreResult().remediationOrder().stream().map(this::toChecklistView).toList(),
                outcome.ruleResult().expertReviewItems().stream()
                        .map(item -> new ExpertReviewView(item.question(), item.reason().name()))
                        .toList(),
                outcome.ruleResult().ruleSetVersion().value(),
                SIMULATION_NOTICE);
    }

    public RemediationPlanResponse toResponse(String diagnosisId, RemediationPlan plan) {
        return new RemediationPlanResponse(
                diagnosisId,
                plan.applicable(),
                plan.currentScore(),
                plan.targetScore(),
                plan.achievable(),
                plan.projectedScore(),
                plan.documentCount(),
                plan.steps().stream().map(this::toStepView).toList(),
                plan.remainingMissing(),
                PLAN_NOTICE);
    }

    private RemediationPlanResponse.StepView toStepView(RemediationStep step) {
        return new RemediationPlanResponse.StepView(
                step.order(),
                step.documentCode().value(),
                step.requirement().name(),
                step.weight(),
                step.scoreAfter(),
                step.gainPercentagePoints());
    }

    private List<String> toCodes(List<DocumentCode> codes) {
        return codes.stream().map(DocumentCode::value).toList();
    }

    private ScoreView toScoreView(ReadinessScore score) {
        if (score == null) {
            return new ScoreView(false, 0, 0, 0);
        }
        return new ScoreView(
                score.applicable(), score.percentage(), score.earnedWeight(), score.totalWeight());
    }

    private CandidateView toCandidateView(CertificationCandidate candidate) {
        List<String> rules = candidate.matchedRules().stream().map(RuleCode::value).sorted().toList();
        return new CandidateView(candidate.schemeCode().value(), candidate.type().name(), rules);
    }

    private ChecklistView toChecklistView(ChecklistItem item) {
        return new ChecklistView(
                item.documentCode().value(), item.requirement().name(), item.weight(),
                item.status().name());
    }
}
