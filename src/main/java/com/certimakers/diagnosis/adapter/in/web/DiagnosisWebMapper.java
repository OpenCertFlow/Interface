package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.CandidateView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ChecklistView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.DegradedView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.EvidenceView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ExpertReviewView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.NarrationView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ScoreView;
import com.certimakers.diagnosis.domain.model.BodyContactType;
import com.certimakers.diagnosis.domain.model.ControllerStatus;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ElectricalSpec;
import com.certimakers.diagnosis.domain.model.HeatingSpec;
import com.certimakers.diagnosis.domain.model.TemperatureSource;
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
                toHeatingSpec(request),
                parse(TargetUser.class, request.targetUser(), "targetUser"),
                parse(SalesChannel.class, request.salesChannel(), "salesChannel"),
                request.materials().stream()
                        .map(value -> parse(MaterialType.class, value, "materials"))
                        .collect(Collectors.toUnmodifiableSet()),
                request.heldDocuments().stream()
                        .map(DocumentCode::of)
                        .collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * 발열 사양을 만든다. 관련 입력이 하나도 없으면 발열 제품이 아닌 것으로 보고 null을 준다.
     *
     * <p>불리언 두 항목은 발열 제품이라면 답이 있어야 하므로, 누락 시 false로 채우지 않고
     * 명시적으로 요구한다 — "모른다"를 "아니다"로 바꾸면 화상 위험 판단이 조용히 뒤집힌다.
     */
    private HeatingSpec toHeatingSpec(DiagnoseRequest request) {
        if (!request.hasHeatingInput()) {
            return null;
        }
        // 발열 제품이라면 신체접촉 방식·온도조절기·온도출처와 안전·세탁 항목은 답이 있어야 한다.
        // 누락 시 기본값으로 뭉개지 않고 명시적으로 요구한다 — "모름"을 "없음"으로 바꾸면 판정이 뒤집힌다.
        BodyContactType bodyContactType = parse(
                BodyContactType.class, request.bodyContactType(), "bodyContactType");
        ControllerStatus controllerStatus = parse(
                ControllerStatus.class, request.controllerStatus(), "controllerStatus");
        TemperatureSource temperatureSource = parse(
                TemperatureSource.class, request.temperatureSource(), "temperatureSource");
        requireBoolean(request.medicalUseClaim(), "의료적 표현 여부");
        requireBoolean(request.autoShutOff(), "자동 전원 차단 여부");
        requireBoolean(request.overheatProtection(), "과열 방지 여부");
        requireBoolean(request.removableCover(), "커버 분리 가능 여부");
        requireBoolean(request.washable(), "세탁 가능 여부");
        requireBoolean(request.separableElectricParts(), "전기부 분리 가능 여부");
        requireBoolean(request.hasSeparateAdapter(), "별도 어댑터 사용 여부");

        // 어댑터 세부 항목은 어댑터가 있을 때만 요구한다. 없으면 null이어야 HeatingSpec 불변식을 지킨다.
        Boolean adapterExternallyAttached = null;
        Boolean adapterCertified = null;
        if (Boolean.TRUE.equals(request.hasSeparateAdapter())) {
            requireBoolean(request.adapterExternallyAttached(), "어댑터 외장 여부");
            requireBoolean(request.adapterCertified(), "어댑터 인증 여부");
            adapterExternallyAttached = request.adapterExternallyAttached();
            adapterCertified = request.adapterCertified();
        }

        // 조절단계·자동꺼짐 시간·표면온도와 출처의 짝 규칙은 HeatingSpec 생성자가 강제한다.
        // 위반 시 도메인의 IllegalArgumentException을 400 메시지로 바꿔 사용자가 고칠 수 있게 한다.
        try {
            return new HeatingSpec(
                    bodyContactType, controllerStatus, request.adjustmentSteps(),
                    request.maxSurfaceTemperatureCelsius(), temperatureSource,
                    request.medicalUseClaim(), request.autoShutOff(), request.autoShutOffMinutes(),
                    request.overheatProtection(), request.removableCover(), request.washable(),
                    request.separableElectricParts(), request.hasSeparateAdapter(),
                    adapterExternallyAttached, adapterCertified);
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid(e.getMessage());
        }
    }

    private void requireBoolean(Boolean value, String label) {
        if (value == null) {
            throw BusinessException.invalid("발열 제품은 %s을(를) 입력해야 합니다.".formatted(label));
        }
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
