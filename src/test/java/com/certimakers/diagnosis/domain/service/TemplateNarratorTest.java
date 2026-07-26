package com.certimakers.diagnosis.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.diagnosis.domain.ProductProfileFixtures;
import com.certimakers.diagnosis.domain.RuleSetFixtures;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import com.certimakers.diagnosis.domain.model.Narration;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemplateNarratorTest {

    private final TemplateNarrator narrator = new TemplateNarrator();
    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();
    private final ScoreCalculator scoreCalculator = new ScoreCalculator();

    private Diagnosis evaluated(ProductProfile profile) {
        Diagnosis diagnosis =
                Diagnosis.request(DiagnosisId.of(com.certimakers.support.TestIds.next()), profile, null, Instant.EPOCH);
        RuleEvaluationResult ruleResult =
                ruleEvaluator.evaluate(profile, RuleSetFixtures.smallApplianceV1());
        ScoreResult scoreResult = scoreCalculator.calculate(
                ruleResult.requiredDocuments(), profile.heldDocuments(), ScoreRubric.defaultsOnly());
        diagnosis.evaluated(ruleResult, scoreResult);
        return diagnosis;
    }

    @Test
    @DisplayName("템플릿 문장은 항상 templateFallback=true로 표시된다")
    void 폴백_표시() {
        Diagnosis diagnosis = evaluated(ProductProfileFixtures.hairDryer(Set.of()));

        Narration narration = narrator.narrate(diagnosis);

        assertThat(narration.isTemplateFallback()).isTrue();
        assertThat(narration.modelId()).isEqualTo("template");
    }

    @Test
    @DisplayName("누락 서류를 가중치 높은 순으로 다음 행동에 담는다")
    void 누락서류_다음행동() {
        // 아무 서류도 보유하지 않은 드라이기 → 요구 서류 전부 누락
        Diagnosis diagnosis = evaluated(ProductProfileFixtures.hairDryer(Set.of()));

        Narration narration = narrator.narrate(diagnosis);

        assertThat(narration.nextActions()).isNotEmpty();
        // 필수 서류(가중치 3)가 권장 서류(가중치 1)보다 먼저 등장해야 한다
        String firstAction = narration.nextActions().get(0);
        assertThat(firstAction).containsAnyOf("BIZ_LICENSE", "TEST_REPORT", "SAFETY_LABEL_SAMPLE");
    }

    @Test
    @DisplayName("전문가 확인 항목은 상담 전 질문 목록으로 옮겨진다")
    void 전문가확인_상담전질문() {
        // 전기 미사용 제품 → NO_MATCHING_RULE 전문가 확인 항목 발생
        Diagnosis diagnosis = evaluated(ProductProfileFixtures.nonElectricProduct());

        Narration narration = narrator.narrate(diagnosis);

        assertThat(narration.preConsultQuestions()).isNotEmpty();
        assertThat(narration.summary()).contains("찾지 못했습니다");
    }

    @Test
    @DisplayName("준비도 점수를 요약 문장에 담는다")
    void 점수_요약() {
        Diagnosis diagnosis = evaluated(
                ProductProfileFixtures.hairDryer(Set.of(RuleSetFixtures.BIZ_LICENSE)));

        Narration narration = narrator.narrate(diagnosis);

        assertThat(narration.summary()).contains("준비도");
    }
}
