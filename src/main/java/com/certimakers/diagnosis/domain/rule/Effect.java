package com.certimakers.diagnosis.domain.rule;

/**
 * 룰이 매칭됐을 때 진단 결과에 미치는 효과.
 *
 * <p>효과는 <b>누적적</b>이다. 여러 룰이 같은 서류를 요구하면 더 강한 요구가 이기고, 같은 인증
 * 후보를 지목하면 매칭 룰이 병합된다(제거가 아니라). 해커톤 범위(20~30개 룰)에서 예외 조건에 의한
 * 후보 제거는 다루지 않으며, 판단 불가는 {@link FlagExpertReview}로 표현한다(ADR-0003).
 *
 * <p>sealed로 닫아 두어 새 효과 타입 추가 시 {@code RuleEvaluator}의 switch가 누락을 컴파일 에러로 잡는다.
 */
public sealed interface Effect
        permits AddCandidate, RequireDocument, AddLabelingCheck, FlagExpertReview {
}
