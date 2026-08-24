package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.model.AggregateRoot;
import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.error.DiagnosisErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 인증 준비 계획(F-APP-049). 진단이 알려준 누락 서류를 사용자가 확보해 가는 목록이다.
 *
 * <p>진단을 수정하지 않고 <b>ID로만 참조</b>한다 — 진단은 평가 시점의 스냅샷이라 나중에 바뀌면
 * 안 된다. 진행률은 완료/전체 카운트이며 준비도 점수를 다시 계산하지 않는다(PM 검토 범위).
 */
public class PrepPlan extends AggregateRoot<PrepPlanId> {

    private final PrepPlanId id;
    private final String ownerUserId;          // diagnosis.owner_user_id와 같은 형태(JWT subject)
    private final DiagnosisId diagnosisId;     // 어느 진단에서 나온 목록인지. ID 참조만
    private final List<PrepItem> items = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;

    private PrepPlan(PrepPlanId id, String ownerUserId, DiagnosisId diagnosisId,
            List<PrepItem> items, Instant createdAt, Instant updatedAt) {
        this.id = Guard.notNull(id, "id");
        this.ownerUserId = Guard.hasText(ownerUserId, "ownerUserId");
        this.diagnosisId = Guard.notNull(diagnosisId, "diagnosisId");
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        this.updatedAt = Guard.notNull(updatedAt, "updatedAt");
        this.items.addAll(Guard.notNull(items, "items"));
    }

    /**
     * 진단의 누락 서류로 새 준비목록을 만든다. 점수는 계산하지 않는다.
     *
     * <p><b>받은 순서를 그대로 보존한다.</b> 무엇부터 준비할지는 {@code Diagnosis.remediationOrder()}가
     * 이미 가중치 내림차순으로 정해 두었고, 그 규칙을 여기서 다시 정의하면 두 곳이 따로 논다.
     *
     * @param missingCodes 진단이 정한 보완 우선순위대로의 누락 서류 코드들
     */
    public static PrepPlan from(PrepPlanId id, String ownerUserId, DiagnosisId diagnosisId,
            List<DocumentCode> missingCodes, Instant now) {
        List<PrepItem> items = Guard.notNull(missingCodes, "missingCodes").stream()
                .map(PrepItem::of)
                .toList();
        return new PrepPlan(id, ownerUserId, diagnosisId, items, now, now);
    }

    /** 저장된 상태에서 되살린다. <b>영속성 재구성 전용</b>이다. */
    public static PrepPlan reconstitute(PrepPlanId id, String ownerUserId, DiagnosisId diagnosisId,
            List<PrepItem> items, Instant createdAt, Instant updatedAt) {
        return new PrepPlan(id, ownerUserId, diagnosisId, items, createdAt, updatedAt);
    }

    /**
     * 항목을 체크·해제한다. 목록에 없는 서류 코드는 거부한다 — 사용자가 임의 코드를 보내
     * 목록을 늘리지 못하게 한다.
     */
    public void check(DocumentCode code, boolean done, Instant now) {
        PrepItem item = items.stream()
                .filter(candidate -> candidate.documentCode().equals(code))
                .findFirst()
                .orElseThrow(() ->
                        new BusinessException(DiagnosisErrorCode.PREP_ITEM_NOT_FOUND));
        item.markDone(done);
        this.updatedAt = Guard.notNull(now, "now");
    }

    /** 소유자 본인인지. 아니면 호출부가 찾을 수 없음으로 다룬다. */
    public boolean isOwnedBy(String requesterUserId) {
        return ownerUserId.equals(requesterUserId);
    }

    public int completed() {
        return (int) items.stream().filter(PrepItem::done).count();
    }

    public int total() {
        return items.size();
    }

    /**
     * 진행률(%). 준비할 항목이 없으면 0이 아니라 <b>산정 불가</b>로 봐야 한다 —
     * {@link #hasItems()}로 구분하고, 0%를 "아무것도 안 했다"로 읽히게 두지 않는다
     * ({@code ReadinessScore.applicable}과 같은 취지).
     */
    public int progress() {
        return total() == 0 ? 0 : Math.round((float) completed() / total() * 100);
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    @Override
    public PrepPlanId id() {
        return id;
    }

    public String ownerUserId() {
        return ownerUserId;
    }

    public DiagnosisId diagnosisId() {
        return diagnosisId;
    }

    /** 밖에서 목록을 직접 바꾸지 못하게 불변 뷰로 준다. 변경은 {@link #check}로만. */
    public List<PrepItem> items() {
        return Collections.unmodifiableList(items);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
