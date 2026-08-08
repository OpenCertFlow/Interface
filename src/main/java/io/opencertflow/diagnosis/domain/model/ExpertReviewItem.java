package io.opencertflow.diagnosis.domain.model;

import io.opencertflow.common.domain.model.Guard;

/**
 * 전문가 확인 필요 항목. 룰·근거로 단정할 수 없어 격리한 질문이다.
 *
 * <p>이 타입이 1급 도메인 개념으로 존재하는 것이 이 서비스의 신뢰성 설계의 핵심이다.
 * 불확실한 것을 확실한 척하지 않고, 상담 전 질문 목록으로 전환한다.
 *
 * @param question  사용자가 전문가에게 물어볼 질문
 * @param reason    격리된 이유
 */
public record ExpertReviewItem(String question, ExpertReviewReason reason) {

    public ExpertReviewItem {
        Guard.hasText(question, "question");
        Guard.notNull(reason, "reason");
    }
}
