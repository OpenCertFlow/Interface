package com.certimakers.diagnosis.domain.model;

import com.certimakers.common.domain.model.Guard;

/**
 * 요구 서류 하나와 그 보유 여부, 그리고 평가 시점의 가중치.
 *
 * <p>{@code weight}는 스냅샷이다. 가중치 기준표를 나중에 조정해도 과거 진단의 점수는 재계산되지
 * 않아야 하므로, 계산에 쓴 값을 항목에 박아 둔다(05-data-model.md).
 *
 * @param documentCode  서류 코드
 * @param requirement   요구 강도
 * @param weight        평가에 사용된 가중치 (기준표에서 온 값)
 * @param held          사용자 보유 여부
 */
public record ChecklistItem(
        DocumentCode documentCode,
        Requirement requirement,
        int weight,
        boolean held) {

    public ChecklistItem {
        Guard.notNull(documentCode, "documentCode");
        Guard.notNull(requirement, "requirement");
        Guard.positive(weight, "weight");
    }

    public boolean isMissing() {
        return !held;
    }
}
