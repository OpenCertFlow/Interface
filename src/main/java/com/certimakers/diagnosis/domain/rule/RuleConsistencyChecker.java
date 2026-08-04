package com.certimakers.diagnosis.domain.rule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 룰셋이 스스로 모순되지 않는지 검사한다. 순수 함수다.
 *
 * <p>기존 검증({@code RuleDefinitionValidatorAdapter})은 <b>JSON이 파싱되는가</b>만 본다.
 * 문법이 맞아도 의미가 깨진 룰은 그대로 통과해 활성화된다. 룰이 20~30개일 때는 사람이 훑을 수
 * 있지만 제품군을 늘리면 100개를 넘어가고, 그때부터 아래가 <b>조용히</b> 생긴다.
 *
 * <ul>
 *   <li><b>절대 발동하지 않는 룰</b> — {@code 전압 > 220 AND 전압 < 100}처럼 어떤 입력으로도
 *       참이 될 수 없다. 작성자는 켜질 거라 믿고 두지만 영영 죽어 있다.
 *   <li><b>중복 코드</b> — 같은 룰 코드가 둘. 뒤엣것이 앞엣것을 덮는지 둘 다 도는지 불분명하다.
 *   <li><b>같은 조건 중복</b> — 조건이 완전히 같은 룰이 여럿. 효과를 한 룰에 합치는 편이 낫고,
 *       나뉘어 있으면 한쪽만 고치는 사고가 난다.
 *   <li><b>효과 없는 룰</b> — 조건은 있는데 낼 것이 없다.
 *   <li><b>미사용 속성</b> — 어떤 룰도 보지 않는 입력 항목. 사용자에게 묻고 있는데 아무 데도
 *       쓰이지 않는다면 질문이 낭비이거나 룰이 빠진 것이다.
 * </ul>
 *
 * <p>여기서 다루지 않는 것: 서로 다른 두 룰이 상반된 효과를 내는지는 판정하지 않는다. 이 시스템의
 * 효과는 누적적이고(서류 요구는 강한 쪽이 이긴다) 상반이라는 개념 자체가 없다. 없는 문제를 찾는
 * 검사를 만들면 거짓 경보만 쌓인다.
 */
public final class RuleConsistencyChecker {

    /** 문제 하나. {@code ruleCode}가 비면 룰셋 전체에 대한 지적이다. */
    public record Finding(Severity severity, String ruleCode, String kind, String message) {
    }

    public enum Severity {
        /** 고치지 않으면 룰이 의도대로 동작하지 않는다. */
        ERROR,
        /** 동작은 하지만 유지보수에서 사고가 나기 쉽다. */
        WARNING
    }

    private RuleConsistencyChecker() {
    }

    public static List<Finding> check(List<Rule> rules) {
        List<Finding> findings = new ArrayList<>();
        if (rules == null || rules.isEmpty()) {
            return findings;
        }
        duplicateCodes(rules, findings);
        for (Rule rule : rules) {
            unsatisfiable(rule, findings);
            emptyEffects(rule, findings);
        }
        duplicateConditions(rules, findings);
        unusedAttributes(rules, findings);
        return List.copyOf(findings);
    }

    // ── 개별 룰 ────────────────────────────────────────────────────

    /**
     * 같은 속성에 대한 요구가 서로를 배제하면 그 논리곱은 절대 참이 될 수 없다.
     *
     * <p>완전한 충족 가능성 판정(SAT)은 하지 않는다. 실제 룰에서 압도적으로 흔한 형태 —
     * 최상위 {@link AllOf} 안의 같은 속성 비교들 — 만 본다. 정확도를 위해 재현율을 포기한 선택이며,
     * 거짓 경보가 하나라도 나오면 팀이 이 검사를 무시하게 되기 때문이다.
     */
    private static void unsatisfiable(Rule rule, List<Finding> findings) {
        if (!(rule.condition() instanceof AllOf allOf)) {
            return;
        }
        Map<Attribute, List<AttributeMatch>> byAttribute = new LinkedHashMap<>();
        for (Condition child : allOf.conditions()) {
            if (child instanceof AttributeMatch match) {
                byAttribute.computeIfAbsent(match.attribute(), key -> new ArrayList<>()).add(match);
            }
        }
        for (Map.Entry<Attribute, List<AttributeMatch>> entry : byAttribute.entrySet()) {
            String conflict = findConflict(entry.getValue());
            if (conflict != null) {
                findings.add(new Finding(Severity.ERROR, rule.code().value(), "UNSATISFIABLE",
                        entry.getKey().name() + " 조건이 서로를 배제해 이 룰은 절대 발동하지 않습니다: "
                                + conflict));
            }
        }
    }

    private static String findConflict(List<AttributeMatch> matches) {
        for (int i = 0; i < matches.size(); i++) {
            for (int j = i + 1; j < matches.size(); j++) {
                String reason = conflictBetween(matches.get(i), matches.get(j));
                if (reason != null) {
                    return reason;
                }
            }
        }
        return null;
    }

    private static String conflictBetween(AttributeMatch left, AttributeMatch right) {
        // 서로 다른 값을 동시에 요구
        if (left.operator() == Operator.EQ && right.operator() == Operator.EQ
                && !Objects.equals(left.value(), right.value())) {
            return "EQ " + left.value() + " 와 EQ " + right.value();
        }
        // 같은 값을 요구하면서 동시에 배제
        if (left.operator() == Operator.EQ && right.operator() == Operator.NEQ
                && Objects.equals(left.value(), right.value())) {
            return "EQ " + left.value() + " 와 NEQ " + right.value();
        }
        if (left.operator() == Operator.NEQ && right.operator() == Operator.EQ
                && Objects.equals(left.value(), right.value())) {
            return "NEQ " + left.value() + " 와 EQ " + right.value();
        }
        // 수치 범위가 어긋남 (하한 >= 상한)
        Integer lower = boundOf(left, true);
        Integer upperFromRight = boundOf(right, false);
        if (lower != null && upperFromRight != null && lower >= upperFromRight) {
            return "하한 " + lower + " 이 상한 " + upperFromRight + " 이상";
        }
        Integer lowerFromRight = boundOf(right, true);
        Integer upper = boundOf(left, false);
        if (lowerFromRight != null && upper != null && lowerFromRight >= upper) {
            return "하한 " + lowerFromRight + " 이 상한 " + upper + " 이상";
        }
        return null;
    }

    /** {@code lower=true}면 하한(GT·GTE), 아니면 상한(LT·LTE)을 꺼낸다. 수치가 아니면 null. */
    private static Integer boundOf(AttributeMatch match, boolean lower) {
        if (!(match.value() instanceof Number number)) {
            return null;
        }
        int value = number.intValue();
        if (lower) {
            if (match.operator() == Operator.GT) {
                return value + 1;
            }
            return match.operator() == Operator.GTE ? value : null;
        }
        if (match.operator() == Operator.LT) {
            return value;
        }
        return match.operator() == Operator.LTE ? value + 1 : null;
    }

    private static void emptyEffects(Rule rule, List<Finding> findings) {
        if (rule.effects().isEmpty()) {
            findings.add(new Finding(Severity.ERROR, rule.code().value(), "NO_EFFECT",
                    "조건은 있으나 낼 효과가 없습니다."));
        }
    }

    // ── 룰셋 전체 ──────────────────────────────────────────────────

    private static void duplicateCodes(List<Rule> rules, List<Finding> findings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Rule rule : rules) {
            counts.merge(rule.code().value(), 1, Integer::sum);
        }
        counts.forEach((code, count) -> {
            if (count > 1) {
                findings.add(new Finding(Severity.ERROR, code, "DUPLICATE_CODE",
                        "같은 룰 코드가 " + count + "번 정의되었습니다."));
            }
        });
    }

    private static void duplicateConditions(List<Rule> rules, List<Finding> findings) {
        Map<Condition, List<String>> byCondition = new LinkedHashMap<>();
        for (Rule rule : rules) {
            byCondition.computeIfAbsent(rule.condition(), key -> new ArrayList<>())
                    .add(rule.code().value());
        }
        byCondition.values().stream()
                .filter(codes -> codes.size() > 1)
                .forEach(codes -> findings.add(new Finding(
                        Severity.WARNING, codes.get(0), "DUPLICATE_CONDITION",
                        "조건이 완전히 같은 룰이 있습니다: " + String.join(", ", codes)
                                + ". 효과를 한 룰로 합치면 한쪽만 고치는 사고를 막습니다.")));
    }

    /**
     * 어떤 룰도 보지 않는 속성을 알린다.
     *
     * <p>{@code WARNING}인 이유: 제품군마다 쓰는 속성이 다르므로 미사용 자체가 오류는 아니다.
     * 다만 사용자에게 묻고 있는 항목이 아무 판단에도 쓰이지 않는다면, 질문이 낭비이거나 룰이
     * 빠진 것이라 한 번은 확인할 값어치가 있다.
     */
    private static void unusedAttributes(List<Rule> rules, List<Finding> findings) {
        Set<Attribute> used = new HashSet<>();
        for (Rule rule : rules) {
            collectAttributes(rule.condition(), used);
        }
        List<String> unused = new ArrayList<>();
        for (Attribute attribute : Attribute.values()) {
            if (!used.contains(attribute)) {
                unused.add(attribute.name());
            }
        }
        if (!unused.isEmpty()) {
            findings.add(new Finding(Severity.WARNING, "", "UNUSED_ATTRIBUTE",
                    "어떤 룰도 사용하지 않는 입력 속성: " + String.join(", ", unused)));
        }
    }

    private static void collectAttributes(Condition condition, Set<Attribute> out) {
        if (condition instanceof AttributeMatch match) {
            out.add(match.attribute());
        } else if (condition instanceof AllOf allOf) {
            allOf.conditions().forEach(child -> collectAttributes(child, out));
        } else if (condition instanceof AnyOf anyOf) {
            anyOf.conditions().forEach(child -> collectAttributes(child, out));
        } else if (condition instanceof Not not) {
            collectAttributes(not.condition(), out);
        }
    }
}
