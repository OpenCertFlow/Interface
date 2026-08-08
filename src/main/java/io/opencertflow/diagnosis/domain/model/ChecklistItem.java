package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.model.Guard;

/**
 * 요구 서류 하나와 그에 대한 사용자의 상태, 그리고 평가 시점의 가중치.
 *
 * <p>{@code weight}는 스냅샷이다. 가중치 기준표를 나중에 조정해도 과거 진단의 점수는 재계산되지
 * 않아야 하므로, 계산에 쓴 값을 항목에 박아 둔다(05-data-model.md).
 *
 * <p>{@code status}가 {@code boolean held}가 아닌 이유는 '모름'을 '없음'으로 뭉개지 않기
 * 위해서다({@link ChecklistStatus}).
 *
 * @param documentCode  서류 코드
 * @param requirement   요구 강도
 * @param weight        평가에 사용된 가중치 (기준표에서 온 값)
 * @param status        보유·미보유·모름
 */
public record ChecklistItem(
        DocumentCode documentCode,
        Requirement requirement,
        int weight,
        ChecklistStatus status) {

    public ChecklistItem {
        Guard.notNull(documentCode, "documentCode");
        Guard.notNull(requirement, "requirement");
        Guard.notNull(status, "status");
        Guard.positive(weight, "weight");
    }

    /**
     * 보유 여부만 아는 호출부를 위한 편의 생성자. '모름' 개념이 없던 시절의 데이터와 테스트를
     * 그대로 쓸 수 있게 남겨 둔다.
     */
    public ChecklistItem(
            DocumentCode documentCode, Requirement requirement, int weight, boolean held) {
        this(documentCode, requirement, weight,
                held ? ChecklistStatus.HELD : ChecklistStatus.MISSING);
    }

    public boolean held() {
        return status == ChecklistStatus.HELD;
    }

    /** 보유하지 않은 모든 상태(없음·모름). 점수에 반영되지 않는다는 뜻이다. */
    public boolean isMissing() {
        return status != ChecklistStatus.HELD;
    }

    /** 사용자가 만들어야 하는 서류. 리포트의 '누락자료'에 해당한다. */
    public boolean isAbsent() {
        return status == ChecklistStatus.MISSING;
    }

    /** 사용자가 확인해야 하는 서류. 리포트의 '확인 중'에 해당한다. */
    public boolean isUnknown() {
        return status == ChecklistStatus.UNKNOWN;
    }
}
