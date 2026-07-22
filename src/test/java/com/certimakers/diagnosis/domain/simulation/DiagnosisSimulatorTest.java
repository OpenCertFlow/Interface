package com.certimakers.diagnosis.domain.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.diagnosis.domain.ProductProfileFixtures;
import com.certimakers.diagnosis.domain.RuleSetFixtures;
import com.certimakers.diagnosis.domain.model.CertificationCandidate;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.TargetUser;
import com.certimakers.diagnosis.domain.rule.RuleSet;
import com.certimakers.diagnosis.domain.service.RuleEvaluationResult;
import com.certimakers.diagnosis.domain.service.RuleEvaluator;
import com.certimakers.diagnosis.domain.service.ScoreCalculator;
import com.certimakers.diagnosis.domain.service.ScoreResult;
import com.certimakers.diagnosis.domain.service.ScoreRubric;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 반사실 시뮬레이션. 결정론적 룰 엔진(ADR-0003) 위에서만 성립하는 기능이므로, "같은 가정은 항상
 * 같은 답을 준다"와 "원본을 건드리지 않는다"가 핵심 검증 대상이다.
 *
 * <p>기준 시나리오: 220V 드라이기, BIZ_LICENSE만 보유. 요구 서류 5종(3·3·3·1·1, 총합 11) 중
 * 3점 보유 → 27%.
 */
class DiagnosisSimulatorTest {

    private final DiagnosisSimulator simulator = new DiagnosisSimulator();
    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();
    private final ScoreCalculator scoreCalculator = new ScoreCalculator();

    private final RuleSet ruleSet = RuleSetFixtures.smallApplianceV1();
    private final ScoreRubric rubric = ScoreRubric.defaultsOnly();

    private final ProductProfile baseProfile =
            ProductProfileFixtures.hairDryer(Set.of(RuleSetFixtures.BIZ_LICENSE));

    private final RuleEvaluationResult baseRuleResult =
            ruleEvaluator.evaluate(baseProfile, ruleSet);
    private final ScoreResult baseScore = scoreCalculator.calculate(
            baseRuleResult.requiredDocuments(), baseProfile.heldDocuments(), rubric);

    private SimulationOutcome simulate(ProfileAdjustment adjustment) {
        return simulator.simulate(
                baseProfile,
                baseScore.score(),
                baseScore.checklist(),
                baseRuleResult.candidates(),
                adjustment,
                ruleSet,
                rubric);
    }

    @Test
    @DisplayName("기준 진단은 27%다 — 이후 테스트의 비교 기준")
    void 기준_진단은_27퍼센트다() {
        assertThat(baseScore.score().percentage()).isEqualTo(27);
        assertThat(baseScore.score().totalWeight()).isEqualTo(11);
    }

    @Nested
    @DisplayName("서류를 준비하면 오르는 폭을 정확히 답한다")
    class DocumentWhatIf {

        @Test
        @DisplayName("가중치 3짜리 서류 하나를 준비하면 27% → 55%")
        void 서류_하나를_준비하면_점수가_정확히_오른다() {
            SimulationOutcome outcome = simulate(
                    ProfileAdjustment.holdingDocuments(Set.of(RuleSetFixtures.TEST_REPORT)));

            assertThat(outcome.scoreDelta().comparable()).isTrue();
            assertThat(outcome.scoreDelta().after().percentage()).isEqualTo(55);
            assertThat(outcome.scoreDelta().percentagePointChange()).isEqualTo(28);
            assertThat(outcome.scoreDelta().improved()).isTrue();
        }

        @Test
        @DisplayName("이번 가정으로 충족된 서류를 짚어 준다")
        void 이번_가정으로_충족된_서류를_짚어준다() {
            SimulationOutcome outcome = simulate(
                    ProfileAdjustment.holdingDocuments(Set.of(RuleSetFixtures.TEST_REPORT)));

            assertThat(outcome.newlySatisfiedDocuments())
                    .extracting(DocumentCode::value)
                    .containsExactly("TEST_REPORT");
        }

        @Test
        @DisplayName("보유 서류를 빼면 점수가 내려간다")
        void 보유_서류를_빼면_점수가_내려간다() {
            SimulationOutcome outcome = simulate(new ProfileAdjustment(
                    Set.of(), Set.of(RuleSetFixtures.BIZ_LICENSE),
                    null, null, null, null, null, null));

            assertThat(outcome.scoreDelta().after().percentage()).isZero();
            assertThat(outcome.scoreDelta().worsened()).isTrue();
        }

        @Test
        @DisplayName("서류만 바꾸면 적용 인증 제도는 그대로다")
        void 서류만_바꾸면_인증_제도는_그대로다() {
            SimulationOutcome outcome = simulate(
                    ProfileAdjustment.holdingDocuments(Set.of(RuleSetFixtures.TEST_REPORT)));

            assertThat(outcome.certificationScopeChanged()).isFalse();
        }
    }

    @Nested
    @DisplayName("제품 사양을 바꾸면 적용 제도 자체가 달라지는 것을 잡아낸다")
    class AttributeWhatIf {

        @Test
        @DisplayName("어린이용으로 바꾸면 어린이제품 안전인증 후보가 새로 생긴다")
        void 어린이용으로_바꾸면_후보가_새로_생긴다() {
            SimulationOutcome outcome = simulate(new ProfileAdjustment(
                    Set.of(), Set.of(), null, null, null, null, TargetUser.CHILD, null));

            assertThat(outcome.certificationScopeChanged()).isTrue();
            assertThat(outcome.addedCandidates())
                    .extracting(candidate -> candidate.schemeCode().value())
                    .containsExactly(RuleSetFixtures.CHILD_SAFETY_CERT.value());
            assertThat(outcome.removedCandidates()).isEmpty();
        }

        @Test
        @DisplayName("전기를 쓰지 않는 것으로 바꾸면 안전확인 후보와 요구 서류가 사라진다")
        void 전기를_쓰지_않으면_후보와_요구_서류가_사라진다() {
            SimulationOutcome outcome = simulate(new ProfileAdjustment(
                    Set.of(), Set.of(), false, null, null, null, null, null));

            assertThat(outcome.removedCandidates())
                    .extracting(candidate -> candidate.schemeCode().value())
                    .containsExactly(RuleSetFixtures.SAFETY_CONFIRM_ELECTRIC.value());
            assertThat(outcome.noLongerRequiredDocuments()).isNotEmpty();
        }

        @Test
        @DisplayName("요구 서류가 사라져 점수를 낼 수 없게 되면 비교 불가로 답한다")
        void 요구_서류가_사라지면_비교_불가로_답한다() {
            SimulationOutcome outcome = simulate(new ProfileAdjustment(
                    Set.of(), Set.of(), false, null, null, null, null, null));

            // 불변식 2: 요구 서류가 없으면 0%가 아니라 산정 불가다. 0%p 변화로 뭉개면 안 된다.
            assertThat(outcome.scoreResult().score().applicable()).isFalse();
            assertThat(outcome.scoreDelta().comparable()).isFalse();
        }

        @Test
        @DisplayName("전기 미사용으로 바꾸면 정격전압·소비전력을 함께 비운다")
        void 전기_미사용으로_바꾸면_전압을_함께_비운다() {
            SimulationOutcome outcome = simulate(new ProfileAdjustment(
                    Set.of(), Set.of(), false, null, null, null, null, null));

            assertThat(outcome.adjustedProfile().electrical().ratedVoltage()).isNull();
            assertThat(outcome.adjustedProfile().electrical().powerConsumption()).isNull();
        }
    }

    @Nested
    @DisplayName("결정론과 무해성을 지킨다")
    class Determinism {

        @Test
        @DisplayName("같은 가정을 두 번 돌리면 완전히 같은 결과가 나온다")
        void 같은_가정은_항상_같은_결과를_낸다() {
            ProfileAdjustment adjustment =
                    ProfileAdjustment.holdingDocuments(Set.of(RuleSetFixtures.TEST_REPORT));

            SimulationOutcome first = simulate(adjustment);
            SimulationOutcome second = simulate(adjustment);

            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("시뮬레이션은 원본 프로파일을 변경하지 않는다")
        void 원본_프로파일을_변경하지_않는다() {
            Set<DocumentCode> heldBefore = Set.copyOf(baseProfile.heldDocuments());

            simulate(ProfileAdjustment.holdingDocuments(Set.of(RuleSetFixtures.TEST_REPORT)));

            assertThat(baseProfile.heldDocuments()).isEqualTo(heldBefore);
        }

        @Test
        @DisplayName("빈 가정은 원본과 같은 결과를 낸다")
        void 빈_가정은_원본과_같은_결과를_낸다() {
            SimulationOutcome outcome = simulate(ProfileAdjustment.holdingDocuments(Set.of()));

            assertThat(outcome.scoreDelta().percentagePointChange()).isZero();
            assertThat(outcome.certificationScopeChanged()).isFalse();
            List<CertificationCandidate> unchanged = outcome.ruleResult().candidates();
            assertThat(unchanged).isEqualTo(baseRuleResult.candidates());
        }
    }
}
