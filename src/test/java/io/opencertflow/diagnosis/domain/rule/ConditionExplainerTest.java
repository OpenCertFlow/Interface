package io.opencertflow.diagnosis.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import io.opencertflow.diagnosis.domain.ProductProfileFixtures;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import io.opencertflow.diagnosis.domain.model.TargetUser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * "왜 이 룰이 켜졌는가"를 설명하는 규칙을 고정한다.
 *
 * <p>핵심은 <b>기여한 가지만</b> 모으는 것이다. 논리합에서 거짓인 가지까지 보여 주면
 * "이것 때문에 걸렸다"는 설명이 흐려진다.
 */
class ConditionExplainerTest {

    /** 전기 사용·220V·배터리 없음. */
    private final ProductProfile dryer = ProductProfileFixtures.hairDryer(Set.of());

    @Test
    @DisplayName("조건이 거짓이면 설명할 것이 없다")
    void 거짓이면_빈_목록() {
        Condition condition = AttributeMatch.of(Attribute.HAS_BATTERY, Operator.EQ, true);

        assertThat(ConditionExplainer.explain(condition, dryer)).isEmpty();
    }

    @Test
    @DisplayName("단말 조건은 기대값과 실제값을 함께 남긴다")
    void 단말_조건은_실제값을_남긴다() {
        Condition condition = AttributeMatch.of(Attribute.USES_ELECTRICITY, Operator.EQ, true);

        List<ConditionFact> facts = ConditionExplainer.explain(condition, dryer);

        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).attribute()).isEqualTo("USES_ELECTRICITY");
        assertThat(facts.get(0).operator()).isEqualTo("EQ");
        assertThat(facts.get(0).expected()).isEqualTo("true");
        assertThat(facts.get(0).actual()).isEqualTo("true");
        assertThat(facts.get(0).negated()).isFalse();
    }

    @Test
    @DisplayName("논리곱은 모든 가지가 기여하므로 전부 남는다")
    void 논리곱은_전부_남는다() {
        Condition condition = AllOf.of(
                AttributeMatch.of(Attribute.USES_ELECTRICITY, Operator.EQ, true),
                AttributeMatch.of(Attribute.RATED_VOLTAGE, Operator.GT, 50));

        List<ConditionFact> facts = ConditionExplainer.explain(condition, dryer);

        assertThat(facts).extracting(ConditionFact::attribute)
                .containsExactly("USES_ELECTRICITY", "RATED_VOLTAGE");
    }

    @Test
    @DisplayName("논리합은 참인 가지만 남긴다 — 거짓 가지는 설명이 아니다")
    void 논리합은_참인_가지만() {
        Condition condition = AnyOf.of(
                AttributeMatch.of(Attribute.HAS_BATTERY, Operator.EQ, true),   // 거짓
                AttributeMatch.of(Attribute.USES_ELECTRICITY, Operator.EQ, true)); // 참

        List<ConditionFact> facts = ConditionExplainer.explain(condition, dryer);

        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).attribute()).isEqualTo("USES_ELECTRICITY");
    }

    @Test
    @DisplayName("부정으로 만족한 조건은 negated로 표시한다")
    void 부정은_표시된다() {
        Condition condition = Not.of(
                AttributeMatch.of(Attribute.HAS_BATTERY, Operator.EQ, true));

        List<ConditionFact> facts = ConditionExplainer.explain(condition, dryer);

        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).negated()).isTrue();
        assertThat(facts.get(0).actual()).isEqualTo("false");
    }

    @Test
    @DisplayName("부정 논리곱은 거짓인 가지가 이유다 — 드모르간을 지킨다")
    void 부정_논리곱은_거짓_가지가_이유() {
        // Not(A and B)가 참인 이유는 A·B 중 거짓인 것이다. 여기서는 배터리 조건이 거짓이다.
        Condition condition = Not.of(AllOf.of(
                AttributeMatch.of(Attribute.USES_ELECTRICITY, Operator.EQ, true), // 참
                AttributeMatch.of(Attribute.HAS_BATTERY, Operator.EQ, true)));    // 거짓

        List<ConditionFact> facts = ConditionExplainer.explain(condition, dryer);

        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).attribute()).isEqualTo("HAS_BATTERY");
        assertThat(facts.get(0).negated()).isTrue();
    }

    @Test
    @DisplayName("IN 조건의 기대값은 목록으로 펼쳐 보여 준다")
    void IN_기대값은_펼쳐진다() {
        ProductProfile withDocs = ProductProfileFixtures.hairDryer(
                Set.of(DocumentCode.of("TEST_REPORT")));
        // 실제 값은 enum이다. 코덱이 JSON 문자열을 enum으로 바꿔 주므로 여기서도 enum으로 만든다.
        Condition condition = AttributeMatch.of(
                Attribute.TARGET_USER, Operator.IN,
                List.of(TargetUser.GENERAL, TargetUser.CHILD));

        List<ConditionFact> facts = ConditionExplainer.explain(condition, withDocs);

        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).expected()).isEqualTo("[GENERAL, CHILD]");
    }
}
