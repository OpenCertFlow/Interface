package com.certimakers.diagnosis.adapter.out.persistence.diagnosis;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code diagnosis} 테이블 매핑 — 애그리거트 루트 엔티티.
 *
 * <p>자식 컬렉션은 {@code cascade = ALL, orphanRemoval = true}로 루트와 생애를 함께한다. 루트를
 * 저장하면 자식이 함께 저장되고, 이것이 "애그리거트를 한 트랜잭션에 통째로"를 실현한다(04-domain-model.md).
 */
@Entity
@Table(name = "diagnosis")
public class DiagnosisEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String status;

    @Column(name = "rule_set_id")
    private Long ruleSetId;

    @Column(name = "rule_set_version")
    private Integer ruleSetVersion;

    @Column(name = "readiness_score")
    private Integer readinessScore;

    @Column(name = "score_applicable", nullable = false)
    private boolean scoreApplicable;

    @Column(name = "earned_weight", nullable = false)
    private int earnedWeight;

    @Column(name = "total_weight", nullable = false)
    private int totalWeight;

    @Column(name = "degraded_evidence", nullable = false)
    private boolean degradedEvidence;

    @Column(name = "degraded_narration", nullable = false)
    private boolean degradedNarration;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** 진단을 요청한 로그인 사용자. 비로그인 진단은 null(익명). '내 진단 이력' 조회의 기준이 된다. */
    @Column(name = "owner_user_id")
    private String ownerUserId;

    /** 이 진단이 어느 진단의 재진단인지. 최초 진단은 null. FK는 V26(ON DELETE SET NULL). */
    @Column(name = "previous_id")
    private Long previousId;

    @OneToOne(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private ProductProfileEntity profile;

    @OneToOne(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true)
    private NarrationEntity narration;

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CertificationCandidateEntity> candidates = new ArrayList<>();

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChecklistItemEntity> checklist = new ArrayList<>();

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LabelingCheckItemEntity> labelingChecks = new ArrayList<>();

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExpertReviewItemEntity> expertReviewItems = new ArrayList<>();

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EvidenceEntity> evidences = new ArrayList<>();

    protected DiagnosisEntity() {
    }

    public DiagnosisEntity(
            Long id, String status, Long ruleSetId, Integer ruleSetVersion, Integer readinessScore,
            boolean scoreApplicable, int earnedWeight, int totalWeight,
            boolean degradedEvidence, boolean degradedNarration, Instant createdAt, Instant updatedAt,
            String ownerUserId, Long previousId) {
        this.id = id;
        this.status = status;
        this.ruleSetId = ruleSetId;
        this.ruleSetVersion = ruleSetVersion;
        this.readinessScore = readinessScore;
        this.scoreApplicable = scoreApplicable;
        this.earnedWeight = earnedWeight;
        this.totalWeight = totalWeight;
        this.degradedEvidence = degradedEvidence;
        this.degradedNarration = degradedNarration;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.ownerUserId = ownerUserId;
        this.previousId = previousId;
    }

    /** 자식과 부모의 양방향 관계를 한 곳에서 맺어, 매퍼가 FK 설정을 잊지 않게 한다. */
    public void attachProfile(ProductProfileEntity profile) {
        profile.setDiagnosis(this);
        this.profile = profile;
    }

    public void attachNarration(NarrationEntity narration) {
        if (narration != null) {
            narration.setDiagnosis(this);
        }
        this.narration = narration;
    }

    public void attachCandidates(List<CertificationCandidateEntity> items) {
        items.forEach(item -> item.setDiagnosis(this));
        this.candidates = items;
    }

    public void attachChecklist(List<ChecklistItemEntity> items) {
        items.forEach(item -> item.setDiagnosis(this));
        this.checklist = items;
    }

    public void attachLabelingChecks(List<LabelingCheckItemEntity> items) {
        items.forEach(item -> item.setDiagnosis(this));
        this.labelingChecks = items;
    }

    public void attachExpertReviewItems(List<ExpertReviewItemEntity> items) {
        items.forEach(item -> item.setDiagnosis(this));
        this.expertReviewItems = items;
    }

    public void attachEvidences(List<EvidenceEntity> items) {
        items.forEach(item -> item.setDiagnosis(this));
        this.evidences = items;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Long getRuleSetId() {
        return ruleSetId;
    }

    public Integer getRuleSetVersion() {
        return ruleSetVersion;
    }

    public Integer getReadinessScore() {
        return readinessScore;
    }

    public boolean isScoreApplicable() {
        return scoreApplicable;
    }

    public int getEarnedWeight() {
        return earnedWeight;
    }

    public int getTotalWeight() {
        return totalWeight;
    }

    public boolean isDegradedEvidence() {
        return degradedEvidence;
    }

    public boolean isDegradedNarration() {
        return degradedNarration;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public Long getPreviousId() {
        return previousId;
    }

    public ProductProfileEntity getProfile() {
        return profile;
    }

    public NarrationEntity getNarration() {
        return narration;
    }

    public List<CertificationCandidateEntity> getCandidates() {
        return candidates;
    }

    public List<ChecklistItemEntity> getChecklist() {
        return checklist;
    }

    public List<LabelingCheckItemEntity> getLabelingChecks() {
        return labelingChecks;
    }

    public List<ExpertReviewItemEntity> getExpertReviewItems() {
        return expertReviewItems;
    }

    public List<EvidenceEntity> getEvidences() {
        return evidences;
    }
}
