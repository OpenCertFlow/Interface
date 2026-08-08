package io.opencertflow.diagnosis.domain.model;

/**
 * 요구 서류 하나에 대한 사용자의 상태.
 *
 * <p>{@code MISSING}과 {@code UNKNOWN}을 나누는 이유는 서비스가 '모름'을 임의로 해석하지 않기
 * 때문이다(운영지침 §9). "없다"와 "있는지 모른다"는 사용자가 해야 할 다음 행동이 다르다 —
 * 전자는 만들어야 하고 후자는 확인해야 한다. 점수에서는 둘 다 획득 가중치에 들어가지 않지만,
 * 리포트에서는 구분해서 보여 준다.
 */
public enum ChecklistStatus {

    /** 보유하고 있다고 체크함. 준비도 점수에 반영된다. */
    HELD,

    /** 보유하지 않음. 준비 대상이다. */
    MISSING,

    /** 보유 여부를 모름·미확인. 확인 대상이며 점수에는 반영하지 않는다. */
    UNKNOWN;

    public boolean earnsWeight() {
        return this == HELD;
    }
}
