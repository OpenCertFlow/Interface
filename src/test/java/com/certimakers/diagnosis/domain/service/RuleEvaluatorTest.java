package com.certimakers.diagnosis.domain.service;

import static com.certimakers.diagnosis.domain.RuleSetFixtures.BIZ_LICENSE;
import static com.certimakers.diagnosis.domain.RuleSetFixtures.CHILD_SAFETY_CERT;
import static com.certimakers.diagnosis.domain.RuleSetFixtures.SAFETY_CONFIRM_ELECTRIC;
import static com.certimakers.diagnosis.domain.RuleSetFixtures.TEST_REPORT;
import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.diagnosis.domain.ProductProfileFixtures;
import com.certimakers.diagnosis.domain.RuleSetFixtures;
import com.certimakers.diagnosis.domain.model.CertificationCandidate;
import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.model.ExpertReviewReason;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.Requirement;
import com.certimakers.diagnosis.domain.model.TargetUser;
import com.certimakers.diagnosis.domain.rule.RuleCode;
import com.certimakers.diagnosis.domain.rule.RuleSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleEvaluatorTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();
    private final RuleSet ruleSet = RuleSetFixtures.smallApplianceV1();

    @Test
    @DisplayName("220V 드라이기 → 안전확인 후보를 식별하고 매칭 룰을 함께 남긴다")
    void 전기제품_안전확인_후보_식별() {
        ProductProfile dryer = ProductProfileFixtures.hairDryer(Set.of());

        RuleEvaluationResult result = evaluator.evaluate(dryer, ruleSet);

        assertThat(result.candidates()).hasSize(1);
        CertificationCandidate candidate = result.candidates().get(0);
        assertThat(candidate.schemeCode()).isEqualTo(SAFETY_CONFIRM_ELECTRIC);
        assertThat(candidate.type()).isEqualTo(CertificationType.SAFETY_CONFIRM);
        // "왜 이 후보인가" — R-SA-001이 근거로 남아야 한다
        assertThat(candidate.matchedRules()).containsExactly(RuleCode.of("R-SA-001"));
    }

    @Test
    @DisplayName("여러 룰이 같은 서류를 요구하면 병합하고, REQUIRED가 RECOMMENDED를 이긴다")
    void 서류_요구_병합_및_강도_우선() {
        ProductProfile dryer = ProductProfileFixtures.hairDryer(Set.of());

        RuleEvaluationResult result = evaluator.evaluate(dryer, ruleSet);

        // R-SA-001(서류 4종) + R-SA-002(라벨 견본 1종) = 서류 5종, 중복 없음
        assertThat(result.requiredDocuments()).hasSize(5);
        assertThat(result.requiredDocuments())
                .anySatisfy(doc -> {
                    assertThat(doc.documentCode()).isEqualTo(BIZ_LICENSE);
                    assertThat(doc.requirement()).isEqualTo(Requirement.REQUIRED);
                });
    }

    @Test
    @DisplayName("표시·라벨링 확인 항목을 도출한다")
    void 라벨링_항목_도출() {
        ProductProfile dryer = ProductProfileFixtures.hairDryer(Set.of());

        RuleEvaluationResult result = evaluator.evaluate(dryer, ruleSet);

        assertThat(result.labelingChecks())
                .extracting(item -> item.label())
                .contains("KC 마크 및 안전확인 표시", "정격전압·소비전력 표시");
    }

    @Test
    @DisplayName("전기 미사용 제품 → 어떤 후보도 없으면 NO_MATCHING_RULE로 격리한다 (불변식 5)")
    void 매칭_없으면_전문가_확인으로_격리() {
        ProductProfile manualComb = ProductProfileFixtures.nonElectricProduct();

        RuleEvaluationResult result = evaluator.evaluate(manualComb, ruleSet);

        assertThat(result.hasCandidate()).isFalse();
        assertThat(result.expertReviewItems())
                .extracting(item -> item.reason())
                .containsExactly(ExpertReviewReason.NO_MATCHING_RULE);
    }

    @Test
    @DisplayName("전압 정보 누락 → 후보를 지어내지 않고 AMBIGUOUS_CONDITION으로 판단 불가를 명시한다")
    void 전압_누락시_판단불가_명시() {
        ProductProfile noVoltage = ProductProfileFixtures.hairDryerWithoutVoltage(Set.of());

        RuleEvaluationResult result = evaluator.evaluate(noVoltage, ruleSet);

        // R-SA-001은 전압>50을 요구하므로 매칭 안 됨 → 후보 없음
        assertThat(result.hasCandidate()).isFalse();
        // R-SA-090이 판단 불가를 명시. 후보 없음으로 NO_MATCHING_RULE도 함께 추가된다.
        assertThat(result.expertReviewItems())
                .extracting(item -> item.reason())
                .contains(ExpertReviewReason.AMBIGUOUS_CONDITION, ExpertReviewReason.NO_MATCHING_RULE);
    }

    @Test
    @DisplayName("어린이용 제품 → 어린이제품 안전인증 후보를 추가로 식별한다")
    void 어린이용_안전인증_후보() {
        ProductProfile childDryer = new ProductProfile(
                "어린이용 드라이기",
                com.certimakers.diagnosis.domain.model.ProductGroup.SMALL_APPLIANCE,
                new com.certimakers.diagnosis.domain.model.ElectricalSpec(true, 220, 800, false),
                TargetUser.CHILD,
                com.certimakers.diagnosis.domain.model.SalesChannel.ONLINE,
                Set.of(com.certimakers.diagnosis.domain.model.MaterialType.PLASTIC),
                Set.of());

        RuleEvaluationResult result = evaluator.evaluate(childDryer, ruleSet);

        // 전기 안전확인 + 어린이 안전인증, 두 후보
        assertThat(result.candidates())
                .extracting(CertificationCandidate::schemeCode)
                .containsExactlyInAnyOrder(SAFETY_CONFIRM_ELECTRIC, CHILD_SAFETY_CERT);
    }

    @Test
    @DisplayName("같은 입력은 항상 같은 결과를 낸다 — 항목 순서까지 동일 (재현성)")
    void 결정론_동일_입력_동일_결과() {
        ProductProfile dryer = ProductProfileFixtures.hairDryer(Set.of(TEST_REPORT));

        RuleEvaluationResult first = evaluator.evaluate(dryer, ruleSet);
        RuleEvaluationResult second = evaluator.evaluate(dryer, ruleSet);

        // record 동일성 — 후보·서류·라벨·전문가확인 목록이 순서까지 완전히 같아야 한다
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("평가에 사용한 룰셋 버전을 결과에 스냅샷으로 남긴다")
    void 룰셋_버전_스냅샷() {
        RuleEvaluationResult result =
                evaluator.evaluate(ProductProfileFixtures.hairDryer(Set.of()), ruleSet);

        assertThat(result.ruleSetVersion().value()).isEqualTo(1);
    }
}
