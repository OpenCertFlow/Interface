package io.opencertflow.diagnosis.adapter.out.persistence.diagnosis;

import io.opencertflow.common.adapter.out.persistence.json.JsonColumns;
import io.opencertflow.diagnosis.domain.model.PowerSource;
import io.opencertflow.diagnosis.domain.model.AdjustmentMode;
import io.opencertflow.diagnosis.domain.model.BodyContactType;
import io.opencertflow.diagnosis.domain.model.CertificationCandidate;
import io.opencertflow.diagnosis.domain.model.CertificationType;
import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.ChecklistStatus;
import io.opencertflow.diagnosis.domain.model.ControllerStatus;
import io.opencertflow.diagnosis.domain.model.TemperatureSource;
import io.opencertflow.diagnosis.domain.model.DegradedFlags;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.DiagnosisStatus;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.ElectricalSpec;
import io.opencertflow.diagnosis.domain.model.HeatingSpec;
import io.opencertflow.diagnosis.domain.model.Evidence;
import io.opencertflow.diagnosis.domain.model.ExpertReviewItem;
import io.opencertflow.diagnosis.domain.model.ExpertReviewReason;
import io.opencertflow.diagnosis.domain.model.LabelingCheckItem;
import io.opencertflow.diagnosis.domain.model.ManufacturingType;
import io.opencertflow.diagnosis.domain.model.MaterialType;
import io.opencertflow.diagnosis.domain.model.Narration;
import io.opencertflow.diagnosis.domain.model.ProductGroup;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import io.opencertflow.diagnosis.domain.model.ReadinessScore;
import io.opencertflow.diagnosis.domain.model.Requirement;
import io.opencertflow.diagnosis.domain.model.SalesChannel;
import io.opencertflow.diagnosis.domain.model.SchemeCode;
import io.opencertflow.diagnosis.domain.model.TargetUser;
import io.opencertflow.diagnosis.domain.rule.RuleCode;
import io.opencertflow.diagnosis.domain.rule.RuleTrace;
import io.opencertflow.diagnosis.domain.rule.RuleSetVersion;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 도메인 {@link Diagnosis} ↔ JPA 엔티티 매핑. 손으로 쓴다.
 *
 * <p>MapStruct 같은 자동 매핑을 쓰지 않는 이유는 클래스 수가 적고, jsonb 컬렉션 변환과 값 객체
 * 재구성이 자동화 이득보다 크기 때문이다(ADR-0001). 매핑 버그는 컴파일러가 아니라 왕복 테스트로 잡는다.
 */
public class DiagnosisMapper {

    // ── 도메인 → 엔티티 ───────────────────────────────────────────

    public DiagnosisEntity toEntity(Diagnosis diagnosis) {
        ReadinessScore score = diagnosis.score();
        DiagnosisEntity entity = new DiagnosisEntity(
                diagnosis.id().value(),
                diagnosis.status().name(),
                null, // rule_set_id: 버전 스냅샷만 저장하고 FK는 걸지 않는다(재현성 우선)
                diagnosis.ruleSetVersion() != null ? diagnosis.ruleSetVersion().value() : null,
                score != null && score.applicable() ? score.percentage() : null,
                score != null && score.applicable(),
                score != null ? score.earnedWeight() : 0,
                score != null ? score.totalWeight() : 0,
                diagnosis.degraded().isEvidenceDegraded(),
                diagnosis.degraded().isNarrationDegraded(),
                diagnosis.createdAt(),
                diagnosis.createdAt(), // 애그리거트는 한 번에 저장되므로 updatedAt = createdAt
                diagnosis.owner().orElse(null),
                // DiagnosisId(값 객체) → Long(컬럼). 없으면 null이 그대로 들어간다.
                diagnosis.previousDiagnosisId().map(DiagnosisId::value).orElse(null));

        entity.setRuleTrace(JsonColumns.write(diagnosis.ruleTraces()));
        entity.attachProfile(toProfileEntity(diagnosis.profile()));
        entity.attachCandidates(diagnosis.candidates().stream().map(this::toCandidateEntity).toList());
        entity.attachChecklist(diagnosis.checklist().stream().map(this::toChecklistEntity).toList());
        entity.attachLabelingChecks(diagnosis.labelingChecks().stream().map(this::toLabelingEntity).toList());
        entity.attachExpertReviewItems(
                diagnosis.expertReviewItems().stream().map(this::toExpertEntity).toList());
        entity.attachEvidences(diagnosis.evidences().stream().map(this::toEvidenceEntity).toList());
        diagnosis.narration().ifPresent(narration -> entity.attachNarration(toNarrationEntity(narration)));
        return entity;
    }

    private ProductProfileEntity toProfileEntity(ProductProfile profile) {
        ElectricalSpec electrical = profile.electrical();
        HeatingSpec heating = profile.heating();

        ProductProfileEntity entity = new ProductProfileEntity(
                profile.productName(),
                profile.productGroup().name(),
                electrical.usesElectricity(),
                electrical.ratedVoltage(),
                electrical.powerConsumption(),
                electrical.hasBattery(),
                profile.targetUser().name(),
                profile.salesChannel().name(),
                JsonColumns.writeStringList(profile.materials().stream().map(Enum::name).toList()),
                JsonColumns.writeStringList(
                        profile.heldDocuments().stream().map(DocumentCode::value).toList()),
                heating != null ? heating.bodyContactType().name() : null,
                heating != null ? heating.controllerStatus().name() : null,
                heating != null ? heating.adjustmentSteps() : null,
                heating != null ? heating.maxSurfaceTemperatureCelsius() : null,
                heating != null ? heating.temperatureSource().name() : null,
                heating != null ? heating.medicalUseClaim() : null,
                heating != null ? heating.autoShutOff() : null,
                heating != null ? heating.autoShutOffMinutes() : null,
                heating != null ? heating.overheatProtection() : null,
                heating != null ? heating.removableCover() : null,
                heating != null ? heating.washable() : null,
                heating != null ? heating.separableElectricParts() : null,
                heating != null ? heating.hasSeparateAdapter() : null,
                heating != null ? heating.adapterExternallyAttached() : null,
                heating != null ? heating.adapterCertified() : null,
                heating != null && heating.adjustmentMode() != null
                        ? heating.adjustmentMode().name() : null,
                heating != null ? heating.temperatureLimitDevice() : null,
                profile.manufacturingType().name(),
                profile.modifiedModel(),
                electrical.powerSource().name());
        entity.setUnknownDocuments(JsonColumns.writeStringList(
                profile.unknownDocuments().stream().map(DocumentCode::value).toList()));
        return entity;
    }

    private CertificationCandidateEntity toCandidateEntity(CertificationCandidate candidate) {
        return new CertificationCandidateEntity(
                candidate.schemeCode().value(),
                candidate.type().name(),
                JsonColumns.writeStringList(candidate.matchedRules().stream().map(RuleCode::value).toList()));
    }

    private ChecklistItemEntity toChecklistEntity(ChecklistItem item) {
        return new ChecklistItemEntity(
                item.documentCode().value(), item.requirement().name(), item.weight(),
                item.status().name());
    }

    private LabelingCheckItemEntity toLabelingEntity(LabelingCheckItem item) {
        return new LabelingCheckItemEntity(
                item.label(),
                JsonColumns.writeStringList(item.matchedRules().stream().map(RuleCode::value).toList()));
    }

    private ExpertReviewItemEntity toExpertEntity(ExpertReviewItem item) {
        return new ExpertReviewItemEntity(item.question(), item.reason().name());
    }

    private EvidenceEntity toEvidenceEntity(Evidence evidence) {
        return new EvidenceEntity(
                evidence.sourceDocumentId(), evidence.sectionType(), evidence.snippet(),
                evidence.sourceUrl().toString(), evidence.relevance());
    }

    private NarrationEntity toNarrationEntity(Narration narration) {
        return new NarrationEntity(
                narration.summary(),
                JsonColumns.writeStringList(narration.nextActions()),
                JsonColumns.writeStringList(narration.preConsultQuestions()),
                narration.disclaimer(),
                narration.modelId(),
                narration.isTemplateFallback());
    }

    // ── 엔티티 → 도메인 ───────────────────────────────────────────

    public Diagnosis toDomain(DiagnosisEntity entity) {
        return Diagnosis.reconstitute(
                DiagnosisId.of(entity.getId()),
                toProfile(entity.getProfile()),
                entity.getOwnerUserId(),
                // Long(컬럼) → DiagnosisId(값 객체). null이면 최초 진단이라 그대로 null.
                entity.getPreviousId() != null ? DiagnosisId.of(entity.getPreviousId()) : null,
                entity.getCreatedAt(),
                DiagnosisStatus.valueOf(entity.getStatus()),
                entity.getRuleSetVersion() != null ? RuleSetVersion.of(entity.getRuleSetVersion()) : null,
                toScore(entity),
                entity.getCandidates().stream().map(this::toCandidate).toList(),
                entity.getChecklist().stream().map(this::toChecklistItem).toList(),
                entity.getLabelingChecks().stream().map(this::toLabelingItem).toList(),
                entity.getExpertReviewItems().stream().map(this::toExpertItem).toList(),
                entity.getEvidences().stream().map(this::toEvidence).toList(),
                entity.getNarration() != null ? toNarration(entity.getNarration()) : null,
                DegradedFlags.of(entity.isDegradedEvidence(), entity.isDegradedNarration()),
                JsonColumns.readList(entity.getRuleTrace(), RuleTrace.class));
    }

    private ProductProfile toProfile(ProductProfileEntity entity) {
        ElectricalSpec electrical = new ElectricalSpec(
                entity.isUsesElectricity(), entity.getRatedVoltage(),
                entity.getPowerConsumption(), entity.isHasBattery(),
                // V31 이전에 저장된 진단은 이 값이 없다. UNKNOWN으로 되살려 "모름"을 보존한다 —
                // AC로 추정해 채우면 재현했을 때 원래 진단과 다른 결과가 나온다.
                entity.getPowerSource() == null
                        ? PowerSource.UNKNOWN
                        : PowerSource.valueOf(entity.getPowerSource()));
        Set<MaterialType> materials = JsonColumns.readStringList(entity.getMaterials()).stream()
                .map(MaterialType::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        Set<DocumentCode> heldDocuments = JsonColumns.readStringList(entity.getHeldDocuments()).stream()
                .map(DocumentCode::of)
                .collect(Collectors.toUnmodifiableSet());
        // 발열 사양이 없는 제품이면 null로 되살린다 — 기본값으로 채우면 발열 룰이 잘못 매칭된다.
        // 발열 상세는 발열 제품이면 저장 시 함께 기록된다. 불리언 세부만 혹시 없으면 false로 되살려
        // 언박싱 NPE를 피하고, enum·조절단계·온도출처는 저장값을 그대로 복원한다.
        HeatingSpec heating = entity.hasHeatingSpec()
                ? new HeatingSpec(
                        BodyContactType.valueOf(entity.getBodyContactType()),
                        ControllerStatus.valueOf(entity.getControllerStatus()),
                        entity.getAdjustmentSteps(),
                        entity.getMaxSurfaceTemperature(),
                        TemperatureSource.valueOf(entity.getTemperatureSource()),
                        Boolean.TRUE.equals(entity.getMedicalUseClaim()),
                        Boolean.TRUE.equals(entity.getAutoShutOff()),
                        entity.getAutoShutOffMinutes(),
                        Boolean.TRUE.equals(entity.getOverheatProtection()),
                        Boolean.TRUE.equals(entity.getRemovableCover()),
                        Boolean.TRUE.equals(entity.getWashable()),
                        Boolean.TRUE.equals(entity.getSeparableElectricParts()),
                        Boolean.TRUE.equals(entity.getHasSeparateAdapter()),
                        entity.getAdapterExternallyAttached(),
                        entity.getAdapterCertified(),
                        entity.getAdjustmentMode() != null
                                ? AdjustmentMode.valueOf(entity.getAdjustmentMode()) : null,
                        Boolean.TRUE.equals(entity.getTemperatureLimitDevice()))
                : null;

        return new ProductProfile(
                entity.getProductName(),
                ProductGroup.valueOf(entity.getProductGroup()),
                electrical,
                heating,
                TargetUser.valueOf(entity.getTargetUser()),
                SalesChannel.valueOf(entity.getSalesChannel()),
                materials,
                heldDocuments,
                JsonColumns.readStringList(entity.getUnknownDocuments()).stream()
                        .map(DocumentCode::of)
                        .collect(Collectors.toUnmodifiableSet()),
                entity.getManufacturingType() != null
                        ? ManufacturingType.valueOf(entity.getManufacturingType())
                        : ManufacturingType.UNKNOWN,
                Boolean.TRUE.equals(entity.getModifiedModel()));
    }

    private ReadinessScore toScore(DiagnosisEntity entity) {
        if (!entity.isScoreApplicable()) {
            return ReadinessScore.notApplicable();
        }
        return new ReadinessScore(
                true, entity.getReadinessScore(), entity.getEarnedWeight(), entity.getTotalWeight());
    }

    private CertificationCandidate toCandidate(CertificationCandidateEntity entity) {
        Set<RuleCode> rules = JsonColumns.readStringList(entity.getMatchedRuleCodes()).stream()
                .map(RuleCode::of)
                .collect(Collectors.toUnmodifiableSet());
        return new CertificationCandidate(
                SchemeCode.of(entity.getSchemeCode()),
                CertificationType.valueOf(entity.getCertificationType()),
                rules);
    }

    private ChecklistItem toChecklistItem(ChecklistItemEntity entity) {
        return new ChecklistItem(
                DocumentCode.of(entity.getDocumentCode()),
                Requirement.valueOf(entity.getRequirement()),
                entity.getWeight(),
                ChecklistStatus.valueOf(entity.getStatus()));
    }

    private LabelingCheckItem toLabelingItem(LabelingCheckItemEntity entity) {
        Set<RuleCode> rules = JsonColumns.readStringList(entity.getMatchedRuleCodes()).stream()
                .map(RuleCode::of)
                .collect(Collectors.toUnmodifiableSet());
        return new LabelingCheckItem(entity.getLabel(), rules);
    }

    private ExpertReviewItem toExpertItem(ExpertReviewItemEntity entity) {
        return new ExpertReviewItem(entity.getQuestion(), ExpertReviewReason.valueOf(entity.getReason()));
    }

    private Evidence toEvidence(EvidenceEntity entity) {
        return new Evidence(
                entity.getSourceDocumentId(), entity.getSectionType(), entity.getSnippet(),
                URI.create(entity.getSourceUrl()), entity.getRelevance());
    }

    private Narration toNarration(NarrationEntity entity) {
        List<String> nextActions = JsonColumns.readStringList(entity.getNextActions());
        List<String> questions = JsonColumns.readStringList(entity.getPreConsultQuestions());
        return new Narration(
                entity.getSummary(), nextActions, questions,
                entity.getDisclaimer(), entity.getModelId(), entity.isTemplateFallback());
    }
}
