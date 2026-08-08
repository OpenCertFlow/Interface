package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.model.ExpertReviewReason;

/**
 * 전문가 확인 필요 항목을 추가한다.
 *
 * <p>이 효과가 있기에 룰은 "모르겠다"를 1급으로 표현할 수 있다. 예컨대 정격전압이 필요한데 값이
 * 없는 경우, 룰은 후보를 지목하는 대신 {@code AMBIGUOUS_CONDITION}으로 이 효과를 낸다.
 */
public record FlagExpertReview(String question, ExpertReviewReason reason) implements Effect {

    public FlagExpertReview {
        Guard.hasText(question, "question");
        Guard.notNull(reason, "reason");
    }
}
