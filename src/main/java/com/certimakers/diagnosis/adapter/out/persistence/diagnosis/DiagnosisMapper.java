package com.certimakers.diagnosis.adapter.out.persistence.diagnosis;

import com.certimakers.common.adapter.out.persistence.json.JsonColumns;
import com.certimakers.diagnosis.domain.model.CertificationCandidate;
import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.model.ChecklistItem;
import com.certimakers.diagnosis.domain.model.DegradedFlags;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.model.DiagnosisStatus;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ElectricalSpec;
import com.certimakers.diagnosis.domain.model.HeatingSpec;
import com.certimakers.diagnosis.domain.model.Evidence;
import com.certimakers.diagnosis.domain.model.ExpertReviewItem;
import com.certimakers.diagnosis.domain.model.ExpertReviewReason;
import com.certimakers.diagnosis.domain.model.LabelingCheckItem;
import com.certimakers.diagnosis.domain.model.MaterialType;
import com.certimakers.diagnosis.domain.model.Narration;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.ReadinessScore;
import com.certimakers.diagnosis.domain.model.Requirement;
import com.certimakers.diagnosis.domain.model.SalesChannel;
import com.certimakers.diagnosis.domain.model.SchemeCode;
import com.certimakers.diagnosis.domain.model.TargetUser;
import com.certimakers.diagnosis.domain.rule.RuleCode;
import com.certimakers.diagnosis.domain.rule.RuleSetVersion;
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
                diagnosis.createdAt()); // 애그리거트는 한 번에 저장되므로 updatedAt = createdAt

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

        return new ProductProfileEntity(
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
                heating != null ? heating.directBodyContact() : null,
                heating != null ? heating.hasTemperatureController() : null,
                heating != null ? heating.maxSurfaceTemperatureCelsius() : null);
    }

    private CertificationCandidateEntity toCandidateEntity(CertificationCandidate candidate) {
        return new CertificationCandidateEntity(
                candidate.schemeCode().value(),
                candidate.type().name(),
                JsonColumns.writeStringList(candidate.matchedRules().stream().map(RuleCode::value).toList()));
    }

    private ChecklistItemEntity toChecklistEntity(ChecklistItem item) {
        return new ChecklistItemEntity(
                item.documentCode().value(), item.requirement().name(), item.weight(), item.held());
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
                DegradedFlags.of(entity.isDegradedEvidence(), entity.isDegradedNarration()));
    }

    private ProductProfile toProfile(ProductProfileEntity entity) {
        ElectricalSpec electrical = new ElectricalSpec(
                entity.isUsesElectricity(), entity.getRatedVoltage(),
                entity.getPowerConsumption(), entity.isHasBattery());
        Set<MaterialType> materials = JsonColumns.readStringList(entity.getMaterials()).stream()
                .map(MaterialType::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        Set<DocumentCode> heldDocuments = JsonColumns.readStringList(entity.getHeldDocuments()).stream()
                .map(DocumentCode::of)
                .collect(Collectors.toUnmodifiableSet());
        // 발열 사양이 없는 제품이면 null로 되살린다 — false로 채우면 발열 룰이 잘못 매칭된다.
        HeatingSpec heating = entity.hasHeatingSpec()
                ? new HeatingSpec(
                        entity.getDirectBodyContact(),
                        entity.getHasTemperatureController(),
                        entity.getMaxSurfaceTemperature())
                : null;

        return new ProductProfile(
                entity.getProductName(),
                ProductGroup.valueOf(entity.getProductGroup()),
                electrical,
                heating,
                TargetUser.valueOf(entity.getTargetUser()),
                SalesChannel.valueOf(entity.getSalesChannel()),
                materials,
                heldDocuments);
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
                entity.isHeld());
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
