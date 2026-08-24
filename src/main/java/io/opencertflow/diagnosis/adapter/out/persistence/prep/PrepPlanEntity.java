package io.opencertflow.diagnosis.adapter.out.persistence.prep;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code prep_plan} 테이블 매핑 — 애그리거트 루트 엔티티.
 *
 * <p>자식 항목은 {@code cascade = ALL, orphanRemoval = true}로 루트와 생애를 함께한다.
 */
@Entity
@Table(name = "prep_plan")
public class PrepPlanEntity {

    @Id
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;

    /**
     * 어느 진단에서 나온 목록인지. FK는 V33(ON DELETE CASCADE).
     *
     * <p>{@code @ManyToOne}이 아니라 생짜 {@code Long}이다 — 진단 전체를 메모리에 끌고 올 이유가
     * 없고, 연관으로 두면 지연 로딩이 블로킹 스케줄러 밖에서 터질 위험이 생긴다(재진단 비교의
     * previous_id와 같은 판단).
     */
    @Column(name = "diagnosis_id", nullable = false)
    private Long diagnosisId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<PrepPlanItemEntity> items = new ArrayList<>();

    protected PrepPlanEntity() {
    }

    public PrepPlanEntity(
            Long id, String ownerUserId, Long diagnosisId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.diagnosisId = diagnosisId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void attachItems(List<PrepPlanItemEntity> attached) {
        attached.forEach(item -> item.setPlan(this));
        this.items = attached;
    }

    /** 항목이 바뀌면 갱신 시각도 함께 움직인다. */
    void touch(Instant at) {
        this.updatedAt = at;
    }

    public Long getId() {
        return id;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public Long getDiagnosisId() {
        return diagnosisId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<PrepPlanItemEntity> getItems() {
        return items;
    }
}
