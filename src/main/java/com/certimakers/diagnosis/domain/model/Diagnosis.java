package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.model.AggregateRoot;
import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.error.DiagnosisErrorCode;
import com.certimakers.diagnosis.domain.rule.RuleSetVersion;
import com.certimakers.diagnosis.domain.service.RuleEvaluationResult;
import com.certimakers.diagnosis.domain.service.ScoreResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 진단 애그리거트 루트. 한 번의 자가진단 결과 전체를 담으며, 한 트랜잭션에서 통째로 저장·조회된다.
 *
 * <p>이 클래스는 상태 전이 규칙과 불변식의 수호자다(04-domain-model.md). 리포트를 구성하는 후보·
 * 체크리스트·라벨링·전문가확인 항목은 룰/점수 서비스가 이미 만들어 주므로, 애그리거트는 그것들을
 * 받아 상태를 진행시키고 규칙을 강제하는 역할에 집중한다.
 *
 * <p>생성 시 {@code id}와 {@code createdAt}을 주입받는다. {@code IdGenerator}·{@code TimeProvider}
 * 포트가 애플리케이션 계층에서 값을 만들어 넘기므로, 도메인은 시각·난수를 직접 만지지 않는다.
 */
public class Diagnosis extends AggregateRoot<DiagnosisId> {

    private final DiagnosisId id;
    private final ProductProfile profile;
    private final Instant createdAt;

    private DiagnosisStatus status;
    private RuleSetVersion ruleSetVersion;
    private ReadinessScore score;
    private final List<CertificationCandidate> candidates = new ArrayList<>();
    private final List<ChecklistItem> checklist = new ArrayList<>();
    private final List<LabelingCheckItem> labelingChecks = new ArrayList<>();
    private final List<ExpertReviewItem> expertReviewItems = new ArrayList<>();
    private final List<Evidence> evidences = new ArrayList<>();
    private Narration narration;
    private final DegradedFlags degraded = new DegradedFlags();

    private Diagnosis(DiagnosisId id, ProductProfile profile, Instant createdAt) {
        this.id = Guard.notNull(id, "id");
        this.profile = Guard.notNull(profile, "profile");
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        this.status = DiagnosisStatus.REQUESTED;
    }

    /** 새 진단을 요청 상태로 시작한다. */
    public static Diagnosis request(DiagnosisId id, ProductProfile profile, Instant createdAt) {
        return new Diagnosis(id, profile, createdAt);
    }

    /**
     * 저장된 상태에서 애그리거트를 되살린다. <b>영속성 재구성 전용</b>이다.
     *
     * <p>상태 전이 메서드(evaluated·complete 등)를 거치지 않고 필드를 직접 채운다. DB의 데이터는
     * 이미 전이 규칙을 통과해 저장된 것이므로 다시 검증하지 않는다. 새 진단을 만들 때는
     * {@link #request}를 쓴다.
     */
    public static Diagnosis reconstitute(
            DiagnosisId id,
            ProductProfile profile,
            Instant createdAt,
            DiagnosisStatus status,
            RuleSetVersion ruleSetVersion,
            ReadinessScore score,
            List<CertificationCandidate> candidates,
            List<ChecklistItem> checklist,
            List<LabelingCheckItem> labelingChecks,
            List<ExpertReviewItem> expertReviewItems,
            List<Evidence> evidences,
            Narration narration,
            DegradedFlags degraded) {

        Diagnosis diagnosis = new Diagnosis(id, profile, createdAt);
        diagnosis.status = Guard.notNull(status, "status");
        diagnosis.ruleSetVersion = ruleSetVersion;
        diagnosis.score = score;
        diagnosis.candidates.addAll(candidates);
        diagnosis.checklist.addAll(checklist);
        diagnosis.labelingChecks.addAll(labelingChecks);
        diagnosis.expertReviewItems.addAll(expertReviewItems);
        diagnosis.evidences.addAll(evidences);
        diagnosis.narration = narration;
        if (degraded.isEvidenceDegraded()) {
            diagnosis.degraded.markEvidence();
        }
        if (degraded.isNarrationDegraded()) {
            diagnosis.degraded.markNarration();
        }
        return diagnosis;
    }

    /**
     * 룰 평가와 점수 산정 결과를 반영해 RULE_EVALUATED로 전이한다. 이 시점에 판정이 확정된다.
     *
     * <p>불변식 5는 {@link RuleEvaluationResult}가 이미 보장한다(후보가 없으면 전문가 확인 항목이 있다).
     * 여기서는 그 결과를 신뢰해 담고, 상태 전이 규칙만 강제한다.
     */
    public void evaluated(RuleEvaluationResult ruleResult, ScoreResult scoreResult) {
        requireStatus(DiagnosisStatus.REQUESTED, "룰 평가");
        Guard.notNull(ruleResult, "ruleResult");
        Guard.notNull(scoreResult, "scoreResult");

        this.ruleSetVersion = ruleResult.ruleSetVersion();
        this.candidates.addAll(ruleResult.candidates());
        this.checklist.addAll(scoreResult.checklist());
        this.labelingChecks.addAll(ruleResult.labelingChecks());
        this.expertReviewItems.addAll(ruleResult.expertReviewItems());
        this.score = scoreResult.score();
        this.status = DiagnosisStatus.RULE_EVALUATED;
    }

    /** RAG가 찾은 근거를 첨부한다. 판정은 바뀌지 않는다(ADR-0003). */
    public void attachEvidences(List<Evidence> found) {
        requireStatus(DiagnosisStatus.RULE_EVALUATED, "근거 첨부");
        this.evidences.addAll(Guard.notNull(found, "evidences"));
    }

    /** LLM(또는 템플릿)이 만든 설명 문장을 첨부한다. */
    public void attachNarration(Narration narration) {
        requireStatus(DiagnosisStatus.RULE_EVALUATED, "문장화 첨부");
        this.narration = Guard.notNull(narration, "narration");
        if (narration.isTemplateFallback()) {
            degraded.markNarration();
        }
    }

    /** 근거 조회가 실패해 근거 없이 진행함을 기록한다. */
    public void markEvidenceDegraded() {
        degraded.markEvidence();
    }

    /**
     * 리포트 구성을 마치고 최종 상태로 전이한다. degraded 플래그에 따라 COMPLETED 또는
     * COMPLETED_DEGRADED가 된다. 판정과 점수는 두 경우 모두 유효하다.
     */
    public void complete() {
        requireStatus(DiagnosisStatus.RULE_EVALUATED, "완료 처리");
        this.status = degraded.any()
                ? DiagnosisStatus.COMPLETED_DEGRADED
                : DiagnosisStatus.COMPLETED;
    }

    /** 보완 우선순위 = 누락 서류를 가중치 내림차순으로. */
    public List<ChecklistItem> remediationOrder() {
        return checklist.stream()
                .filter(ChecklistItem::isMissing)
                .sorted(java.util.Comparator.comparingInt(ChecklistItem::weight).reversed()
                        .thenComparing(item -> item.documentCode().value()))
                .toList();
    }

    private void requireStatus(DiagnosisStatus expected, String action) {
        if (status != expected) {
            throw new BusinessException(
                    DiagnosisErrorCode.INVALID_STATE_TRANSITION,
                    "%s은(는) %s 상태에서만 가능합니다. (현재: %s)".formatted(action, expected, status),
                    java.util.Map.of("expected", expected.name(), "actual", status.name()));
        }
    }

    @Override
    public DiagnosisId id() {
        return id;
    }

    public ProductProfile profile() {
        return profile;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public DiagnosisStatus status() {
        return status;
    }

    public RuleSetVersion ruleSetVersion() {
        return ruleSetVersion;
    }

    public ReadinessScore score() {
        return score;
    }

    public List<CertificationCandidate> candidates() {
        return Collections.unmodifiableList(candidates);
    }

    public List<ChecklistItem> checklist() {
        return Collections.unmodifiableList(checklist);
    }

    public List<LabelingCheckItem> labelingChecks() {
        return Collections.unmodifiableList(labelingChecks);
    }

    public List<ExpertReviewItem> expertReviewItems() {
        return Collections.unmodifiableList(expertReviewItems);
    }

    public List<Evidence> evidences() {
        return Collections.unmodifiableList(evidences);
    }

    public java.util.Optional<Narration> narration() {
        return java.util.Optional.ofNullable(narration);
    }

    public DegradedFlags degraded() {
        return degraded;
    }
}
