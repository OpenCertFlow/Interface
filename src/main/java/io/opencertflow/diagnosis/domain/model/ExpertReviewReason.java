package io.opencertflow.diagnosis.domain.model;

/**
 * 어떤 항목이 "전문가 확인 필요"로 격리된 이유.
 *
 * <p>이 enum이 존재한다는 것 자체가 이 서비스의 신뢰성 태도다. 시스템이 단정할 수 없을 때 침묵하거나
 * 지어내는 대신, 왜 판단하지 못했는지를 분류해 사용자에게 알린다(ADR-0003).
 */
public enum ExpertReviewReason {

    /** 어떤 룰도 이 제품에 매칭되지 않았다. 룰이 모르는 제품이다. */
    NO_MATCHING_RULE("적용 가능한 규칙 없음"),

    /** 룰은 매칭됐지만 RAG가 임계값 이상의 공식 근거를 찾지 못했다. */
    NO_EVIDENCE("공식 근거 확인 불가"),

    /** 판별에 필요한 입력값(정격전압 등)이 없어 조건을 확정할 수 없다. */
    AMBIGUOUS_CONDITION("입력 정보 부족으로 조건 판단 불가");

    private final String displayName;

    ExpertReviewReason(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
