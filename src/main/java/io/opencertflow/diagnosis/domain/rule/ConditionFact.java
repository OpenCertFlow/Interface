package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.common.domain.model.Guard;

/**
 * 룰이 발동한 이유 하나. "어떤 속성이 어떤 값이어서 이 조건을 만족했는가"를 담는다.
 *
 * <p>기획서 3.2는 설명가능성을 "AI가 이렇게 판단했다"는 문구가 아니라 <b>입력 조건 → 적용 Rule →
 * 근거 문서 → 안내문</b>의 추적으로 정의한다. 그동안 응답에는 발동한 룰 코드만 있었고 "왜 그 룰이
 * 켜졌는가"가 없었다. 이 레코드가 그 빈자리를 메운다.
 *
 * @param attribute 검사한 속성 (예: {@code BODY_CONTACT_TYPE})
 * @param operator  비교 연산자
 * @param expected  룰이 기대한 값. 문자열로 평탄화한다 — 화면에 그대로 보여줄 값이고,
 *                  타입을 살려 두면 응답 스키마가 속성 종류만큼 갈라진다
 * @param actual    사용자 입력에서 꺼낸 실제 값. 값이 없으면 {@code null}
 * @param negated   {@code Not} 아래에 있었는지. "직접 접촉이 <b>아닐</b> 것"처럼 부정으로 만족한 조건
 */
public record ConditionFact(
        String attribute,
        String operator,
        String expected,
        String actual,
        boolean negated) {

    public ConditionFact {
        Guard.hasText(attribute, "attribute");
        Guard.hasText(operator, "operator");
    }
}
