package com.certimakers.diagnosis.domain.service;

import static com.certimakers.diagnosis.domain.rule.Attribute.DIRECT_BODY_CONTACT;
import static com.certimakers.diagnosis.domain.rule.Attribute.HAS_TEMPERATURE_CONTROLLER;
import static com.certimakers.diagnosis.domain.rule.Attribute.MAX_SURFACE_TEMPERATURE;
import static com.certimakers.diagnosis.domain.rule.Attribute.USES_ELECTRICITY;
import static com.certimakers.diagnosis.domain.rule.Operator.EQ;
import static com.certimakers.diagnosis.domain.rule.Operator.GT;
import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ElectricalSpec;
import com.certimakers.diagnosis.domain.model.ExpertReviewReason;
import com.certimakers.diagnosis.domain.model.HeatingSpec;
import com.certimakers.diagnosis.domain.model.MaterialType;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.SalesChannel;
import com.certimakers.diagnosis.domain.model.TargetUser;
import com.certimakers.diagnosis.domain.rule.AllOf;
import com.certimakers.diagnosis.domain.rule.AttributeMatch;
import com.certimakers.diagnosis.domain.rule.FlagExpertReview;
import com.certimakers.diagnosis.domain.rule.Rule;
import com.certimakers.diagnosis.domain.rule.RuleCode;
import com.certimakers.diagnosis.domain.rule.RuleSet;
import com.certimakers.diagnosis.domain.rule.RuleSetVersion;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 발열 속성이 룰 조건에서 실제로 동작하는지 검증한다.
 *
 * <p>핵심은 <b>"발열 제품이 아니다"와 "발열 제품인데 값을 모른다"를 구분</b>하는 것이다. 전자를
 * false로 뭉개면 발열 룰이 드라이기에 매칭되고, 후자를 false로 뭉개면 "온도조절기 없음" 경고가
 * 잘못 뜬다. 둘 다 사용자에게 틀린 안내를 하게 된다.
 */
class HeatingRuleEvaluationTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();

    // ── 픽스처 ────────────────────────────────────────────────────

    /** 220V 전기방석. 신체에 닿고 온도조절기가 있으며 표면온도를 측정했다. */
    private static ProductProfile heatingPad(
            boolean hasController, Integer surfaceTemperature) {
        return new ProductProfile(
                "보온용 전기방석",
                ProductGroup.ELECTRIC_HEATING_PAD,
                new ElectricalSpec(true, 220, 60, false),
                new HeatingSpec(true, hasController, surfaceTemperature),
                TargetUser.GENERAL,
                SalesChannel.ONLINE,
                Set.of(MaterialType.TEXTILE),
                Set.of());
    }

    /** 발열 사양이 없는 드라이기. HeatingSpec 자체를 갖지 않는다. */
    private static ProductProfile hairDryer() {
        return new ProductProfile(
                "가정용 헤어드라이어",
                ProductGroup.SMALL_APPLIANCE,
                new ElectricalSpec(true, 220, 1200, false),
                TargetUser.GENERAL,
                SalesChannel.ONLINE,
                Set.of(MaterialType.PLASTIC),
                Set.of(DocumentCode.of("BIZ_LICENSE")));
    }

    /** 전기 사용 + 신체 접촉 → 전문가 확인 (전기방석 뼈대 룰과 같은 형태) */
    private static RuleSet bodyContactRuleSet() {
        return ruleSetOf(new Rule(
                RuleCode.of("R-EH-001"), 10,
                AllOf.of(
                        AttributeMatch.of(USES_ELECTRICITY, EQ, true),
                        AttributeMatch.of(DIRECT_BODY_CONTACT, EQ, true)),
                List.of(new FlagExpertReview(
                        "신체에 닿는 발열 제품입니다.", ExpertReviewReason.NO_EVIDENCE))));
    }

    private static RuleSet ruleSetOf(Rule... rules) {
        return new RuleSet(
                RuleSetVersion.of(1), ProductGroup.ELECTRIC_HEATING_PAD, List.of(rules));
    }

    // ── 검증 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("발열 사양 유무를 구분한다")
    class HeatingSpecPresence {

        @Test
        @DisplayName("신체에 닿는 발열 제품은 룰에 매칭된다")
        void 발열_제품은_매칭된다() {
            RuleEvaluationResult result =
                    evaluator.evaluate(heatingPad(true, 45), bodyContactRuleSet());

            assertThat(result.expertReviewItems())
                    .extracting(item -> item.reason())
                    .contains(ExpertReviewReason.NO_EVIDENCE);
        }

        @Test
        @DisplayName("발열 사양이 없는 드라이기는 발열 룰에 매칭되지 않는다")
        void 발열_사양이_없으면_매칭되지_않는다() {
            RuleEvaluationResult result = evaluator.evaluate(hairDryer(), bodyContactRuleSet());

            // 매칭된 룰이 없으므로 후보도 없고, 불변식 5에 따라 NO_MATCHING_RULE만 남는다.
            assertThat(result.candidates()).isEmpty();
            assertThat(result.expertReviewItems())
                    .extracting(item -> item.reason())
                    .containsExactly(ExpertReviewReason.NO_MATCHING_RULE);
        }
    }

    @Nested
    @DisplayName("온도조절기 유무로 분기한다")
    class TemperatureController {

        private RuleSet noControllerRuleSet() {
            return ruleSetOf(new Rule(
                    RuleCode.of("R-EH-002"), 20,
                    AllOf.of(
                            AttributeMatch.of(DIRECT_BODY_CONTACT, EQ, true),
                            AttributeMatch.of(HAS_TEMPERATURE_CONTROLLER, EQ, false)),
                    List.of(new FlagExpertReview(
                            "온도조절기가 없습니다.", ExpertReviewReason.NO_EVIDENCE))));
        }

        @Test
        @DisplayName("온도조절기가 없으면 과열 확인 항목이 뜬다")
        void 온도조절기가_없으면_경고한다() {
            RuleEvaluationResult result =
                    evaluator.evaluate(heatingPad(false, 45), noControllerRuleSet());

            assertThat(result.expertReviewItems())
                    .extracting(item -> item.question())
                    .anyMatch(question -> question.contains("온도조절기"));
        }

        @Test
        @DisplayName("온도조절기가 있으면 그 경고는 뜨지 않는다")
        void 온도조절기가_있으면_경고하지_않는다() {
            RuleEvaluationResult result =
                    evaluator.evaluate(heatingPad(true, 45), noControllerRuleSet());

            assertThat(result.expertReviewItems())
                    .extracting(item -> item.question())
                    .noneMatch(question -> question.contains("온도조절기"));
        }

        @Test
        @DisplayName("발열 제품이 아니면 온도조절기 경고가 뜨지 않는다 — null을 false로 뭉개지 않는다")
        void 발열_제품이_아니면_경고하지_않는다() {
            RuleEvaluationResult result = evaluator.evaluate(hairDryer(), noControllerRuleSet());

            assertThat(result.expertReviewItems())
                    .extracting(item -> item.question())
                    .noneMatch(question -> question.contains("온도조절기"));
        }
    }

    @Nested
    @DisplayName("표면온도를 모르면 판단 불가로 보낸다")
    class UnknownSurfaceTemperature {

        private RuleSet unknownTemperatureRuleSet() {
            return ruleSetOf(new Rule(
                    RuleCode.of("R-EH-090"), 90,
                    AllOf.of(
                            AttributeMatch.of(DIRECT_BODY_CONTACT, EQ, true),
                            AttributeMatch.of(MAX_SURFACE_TEMPERATURE, EQ, null)),
                    List.of(new FlagExpertReview(
                            "최고 표면온도를 확인해 주세요.", ExpertReviewReason.AMBIGUOUS_CONDITION))));
        }

        @Test
        @DisplayName("표면온도를 입력하지 않으면 전문가 확인으로 보낸다 — 진단 자체를 막지 않는다")
        void 표면온도를_모르면_전문가_확인으로_보낸다() {
            RuleEvaluationResult result =
                    evaluator.evaluate(heatingPad(true, null), unknownTemperatureRuleSet());

            assertThat(result.expertReviewItems())
                    .extracting(item -> item.reason())
                    .contains(ExpertReviewReason.AMBIGUOUS_CONDITION);
        }

        @Test
        @DisplayName("표면온도를 입력하면 그 항목은 뜨지 않는다")
        void 표면온도를_입력하면_뜨지_않는다() {
            RuleEvaluationResult result =
                    evaluator.evaluate(heatingPad(true, 45), unknownTemperatureRuleSet());

            assertThat(result.expertReviewItems())
                    .extracting(item -> item.question())
                    .noneMatch(question -> question.contains("표면온도"));
        }

        @Test
        @DisplayName("표면온도 비교 연산도 동작한다")
        void 표면온도_비교가_동작한다() {
            RuleSet hotSurface = ruleSetOf(new Rule(
                    RuleCode.of("R-EH-050"), 50,
                    AttributeMatch.of(MAX_SURFACE_TEMPERATURE, GT, 50),
                    List.of(new FlagExpertReview(
                            "표면온도가 높습니다.", ExpertReviewReason.NO_EVIDENCE))));

            assertThat(evaluator.evaluate(heatingPad(true, 70), hotSurface).expertReviewItems())
                    .extracting(item -> item.question())
                    .anyMatch(question -> question.contains("표면온도가 높습니다"));

            assertThat(evaluator.evaluate(heatingPad(true, 40), hotSurface).expertReviewItems())
                    .extracting(item -> item.question())
                    .noneMatch(question -> question.contains("표면온도가 높습니다"));
        }
    }

    @Test
    @DisplayName("같은 입력은 같은 결과를 낸다 — 발열 속성이 늘어도 결정론이 유지된다")
    void 결정론이_유지된다() {
        ProductProfile profile = heatingPad(true, 45);
        RuleSet ruleSet = bodyContactRuleSet();

        assertThat(evaluator.evaluate(profile, ruleSet))
                .isEqualTo(evaluator.evaluate(profile, ruleSet));
    }
}
