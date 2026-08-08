package io.opencertflow.diagnosis.domain.rule;

import io.opencertflow.diagnosis.domain.model.ProductProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 발동한 룰이 <b>왜</b> 발동했는지를 단말 조건 단위로 풀어낸다. 순수 함수다.
 *
 * <p>{@link Condition#test}는 참·거짓만 돌려주므로 "무엇 때문에 참이었는가"가 남지 않는다.
 * 여기서는 <b>매칭에 실제로 기여한 가지만</b> 따라 내려가며 단말을 모은다.
 *
 * <ul>
 *   <li>{@link AllOf} — 전부 참이어야 하므로 모든 자식이 기여한다.
 *   <li>{@link AnyOf} — 하나만 참이면 되므로 <b>참인 자식만</b> 기여한다. 거짓인 가지까지 보여 주면
 *       "이것 때문에 걸렸다"는 설명이 흐려진다.
 *   <li>{@link Not} — 부정 문맥을 뒤집어 전달하고, 단말에 {@code negated}로 표시한다.
 * </ul>
 *
 * <p>거짓인 조건은 수집하지 않는다. 이 클래스의 목적은 "왜 걸렸는가"이지 "왜 안 걸렸는가"가
 * 아니다. 후자는 룰 정합성 검사({@link RuleConsistencyChecker})가 다룬다.
 */
public final class ConditionExplainer {

    private ConditionExplainer() {
    }

    /** 조건이 참이 아니면 빈 목록. 참이면 매칭에 기여한 단말들을 순서대로 돌려준다. */
    public static List<ConditionFact> explain(Condition condition, ProductProfile profile) {
        if (condition == null || !condition.test(profile)) {
            return List.of();
        }
        List<ConditionFact> facts = new ArrayList<>();
        collect(condition, profile, false, facts);
        return List.copyOf(facts);
    }

    private static void collect(
            Condition condition, ProductProfile profile, boolean negated,
            List<ConditionFact> out) {

        // Java 17이라 switch 패턴 매칭을 쓸 수 없다. sealed 계층이 네 갈래뿐이라 instanceof로 충분하다.
        if (condition instanceof AttributeMatch match) {
            out.add(toFact(match, profile, negated));
        } else if (condition instanceof AllOf allOf) {
            // 부정 문맥에서는 AND와 OR의 역할이 뒤바뀐다(드모르간). 기여한 가지를 고르는 기준도
            // 함께 뒤집어야 "왜 참인가"가 맞는다.
            descend(allOf.conditions(), profile, negated, negated, out);
        } else if (condition instanceof AnyOf anyOf) {
            descend(anyOf.conditions(), profile, negated, !negated, out);
        } else if (condition instanceof Not not) {
            collect(not.condition(), profile, !negated, out);
        }
    }

    /**
     * @param onlyContributing 참인(부정 문맥이면 거짓인) 자식만 따라갈지. 논리합에서만 켠다
     */
    private static void descend(
            List<Condition> conditions, ProductProfile profile, boolean negated,
            boolean onlyContributing, List<ConditionFact> out) {

        for (Condition child : conditions) {
            if (onlyContributing && child.test(profile) == negated) {
                continue;
            }
            collect(child, profile, negated, out);
        }
    }

    private static ConditionFact toFact(
            AttributeMatch match, ProductProfile profile, boolean negated) {
        return new ConditionFact(
                match.attribute().name(),
                match.operator().name(),
                render(match.value()),
                render(match.attribute().resolve(profile)),
                negated);
    }

    /**
     * 값을 화면에 그대로 쓸 수 있는 문자열로 평탄화한다.
     *
     * <p>컬렉션은 원소를 나열한다 — {@code IN} 조건의 기대값이 목록이라 {@code [Ljava.lang.Object}
     * 같은 것이 새어 나가면 안 된다.
     */
    private static String render(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        return String.valueOf(value);
    }
}
