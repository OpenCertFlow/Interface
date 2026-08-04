package com.certimakers.diagnosis.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.model.SchemeCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 룰셋 정합성 검사의 경계를 고정한다.
 *
 * <p>가장 중요한 성질은 <b>거짓 경보를 내지 않는 것</b>이다. 잘 쓴 룰에 경고가 붙으면 팀이 이
 * 검사를 무시하게 되고, 그러면 진짜 문제도 함께 묻힌다.
 */
class RuleConsistencyCheckerTest {

    private static Rule rule(String code, Condition condition) {
        return new Rule(RuleCode.of(code), 10, condition,
                List.of(new AddCandidate(
                        SchemeCode.of("KC_TEST"), CertificationType.SAFETY_CONFIRM)));
    }

    private static List<RuleConsistencyChecker.Finding> errorsOf(List<Rule> rules) {
        return RuleConsistencyChecker.check(rules).stream()
                .filter(f -> f.severity() == RuleConsistencyChecker.Severity.ERROR)
                .toList();
    }

    @Test
    @DisplayName("정상 룰에는 오류를 붙이지 않는다")
    void 정상_룰은_통과() {
        List<Rule> rules = List.of(
                rule("R-1", AllOf.of(
                        AttributeMatch.of(Attribute.USES_ELECTRICITY, Operator.EQ, true),
                        AttributeMatch.of(Attribute.RATED_VOLTAGE, Operator.GT, 50))),
                rule("R-2", AttributeMatch.of(Attribute.HAS_BATTERY, Operator.EQ, true)));

        assertThat(errorsOf(rules)).isEmpty();
    }

    @Test
    @DisplayName("같은 속성에 서로 다른 값을 동시에 요구하면 절대 발동하지 않는다")
    void 상반된_EQ는_잡는다() {
        List<Rule> rules = List.of(rule("R-DEAD", AllOf.of(
                AttributeMatch.of(Attribute.TARGET_USER, Operator.EQ, "GENERAL"),
                AttributeMatch.of(Attribute.TARGET_USER, Operator.EQ, "CHILD"))));

        assertThat(errorsOf(rules))
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.kind()).isEqualTo("UNSATISFIABLE");
                    assertThat(finding.ruleCode()).isEqualTo("R-DEAD");
                });
    }

    @Test
    @DisplayName("수치 범위가 어긋나면 절대 발동하지 않는다")
    void 어긋난_범위는_잡는다() {
        List<Rule> rules = List.of(rule("R-RANGE", AllOf.of(
                AttributeMatch.of(Attribute.RATED_VOLTAGE, Operator.GT, 220),
                AttributeMatch.of(Attribute.RATED_VOLTAGE, Operator.LT, 100))));

        assertThat(errorsOf(rules)).singleElement()
                .satisfies(f -> assertThat(f.kind()).isEqualTo("UNSATISFIABLE"));
    }

    @Test
    @DisplayName("겹치는 정상 범위는 오류가 아니다")
    void 겹치는_범위는_통과() {
        List<Rule> rules = List.of(rule("R-OK", AllOf.of(
                AttributeMatch.of(Attribute.RATED_VOLTAGE, Operator.GT, 50),
                AttributeMatch.of(Attribute.RATED_VOLTAGE, Operator.LT, 250))));

        assertThat(errorsOf(rules)).isEmpty();
    }

    @Test
    @DisplayName("같은 값을 요구하면서 동시에 배제하면 절대 발동하지 않는다")
    void EQ와_NEQ_충돌을_잡는다() {
        List<Rule> rules = List.of(rule("R-CONTRA", AllOf.of(
                AttributeMatch.of(Attribute.HAS_BATTERY, Operator.EQ, true),
                AttributeMatch.of(Attribute.HAS_BATTERY, Operator.NEQ, true))));

        assertThat(errorsOf(rules)).singleElement()
                .satisfies(f -> assertThat(f.kind()).isEqualTo("UNSATISFIABLE"));
    }

    @Test
    @DisplayName("같은 룰 코드가 두 번 정의되면 오류다")
    void 중복_코드를_잡는다() {
        List<Rule> rules = List.of(
                rule("R-SAME", AttributeMatch.of(Attribute.HAS_BATTERY, Operator.EQ, true)),
                rule("R-SAME", AttributeMatch.of(Attribute.USES_ELECTRICITY, Operator.EQ, true)));

        assertThat(errorsOf(rules)).singleElement()
                .satisfies(f -> assertThat(f.kind()).isEqualTo("DUPLICATE_CODE"));
    }

    @Test
    @DisplayName("조건이 완전히 같은 룰은 경고한다 — 동작은 하지만 한쪽만 고치는 사고가 난다")
    void 같은_조건은_경고() {
        Condition same = AttributeMatch.of(Attribute.USES_ELECTRICITY, Operator.EQ, true);
        List<Rule> rules = List.of(rule("R-A", same), rule("R-B", same));

        List<RuleConsistencyChecker.Finding> findings = RuleConsistencyChecker.check(rules);

        assertThat(findings)
                .filteredOn(f -> f.kind().equals("DUPLICATE_CONDITION"))
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.severity()).isEqualTo(RuleConsistencyChecker.Severity.WARNING);
                    assertThat(f.message()).contains("R-A", "R-B");
                });
    }

    @Test
    @DisplayName("아무도 쓰지 않는 입력 속성을 알린다 — 질문이 낭비이거나 룰이 빠진 것이다")
    void 미사용_속성을_알린다() {
        List<Rule> rules = List.of(
                rule("R-1", AttributeMatch.of(Attribute.USES_ELECTRICITY, Operator.EQ, true)));

        assertThat(RuleConsistencyChecker.check(rules))
                .filteredOn(f -> f.kind().equals("UNUSED_ATTRIBUTE"))
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.severity()).isEqualTo(RuleConsistencyChecker.Severity.WARNING);
                    assertThat(f.message()).contains("HAS_BATTERY");
                    assertThat(f.message()).doesNotContain("USES_ELECTRICITY");
                });
    }

    @Test
    @DisplayName("빈 룰셋은 검사할 것이 없다")
    void 빈_룰셋() {
        assertThat(RuleConsistencyChecker.check(List.of())).isEmpty();
        assertThat(RuleConsistencyChecker.check(null)).isEmpty();
    }
}
