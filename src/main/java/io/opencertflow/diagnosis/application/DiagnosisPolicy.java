package io.opencertflow.diagnosis.application;

import io.opencertflow.common.domain.model.Guard;
import java.time.Duration;

/**
 * 진단 오케스트레이션의 정책 값. 외부 호출의 응답 시간 예산을 담는다.
 *
 * <p>이 값은 어댑터의 설정(AiWorkerProperties)에서 오지만, 서비스가 어댑터에 의존하지 않도록
 * 애플리케이션 계층의 이 record로 감싸 주입한다. 타임아웃 초과는 폴백을 촉발한다 — 즉 정책이다
 * (03-diagnosis-flow.md).
 *
 * @param searchTimeout  RAG 근거 검색 응답 예산 (기본 2초)
 * @param narrateTimeout LLM 문장화 응답 예산 (기본 5초)
 */
public record DiagnosisPolicy(Duration searchTimeout, Duration narrateTimeout) {

    public DiagnosisPolicy {
        Guard.notNull(searchTimeout, "searchTimeout");
        Guard.notNull(narrateTimeout, "narrateTimeout");
        Guard.isTrue(!searchTimeout.isNegative() && !searchTimeout.isZero(),
                "searchTimeout은 양수여야 합니다.");
        Guard.isTrue(!narrateTimeout.isNegative() && !narrateTimeout.isZero(),
                "narrateTimeout은 양수여야 합니다.");
    }
}
