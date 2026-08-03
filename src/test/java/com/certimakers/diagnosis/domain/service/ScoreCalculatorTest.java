package com.certimakers.diagnosis.domain.service;

import static com.certimakers.diagnosis.domain.RuleSetFixtures.BIZ_LICENSE;
import static com.certimakers.diagnosis.domain.RuleSetFixtures.CIRCUIT_DIAGRAM;
import static com.certimakers.diagnosis.domain.RuleSetFixtures.TEST_REPORT;
import static org.assertj.core.api.Assertions.assertThat;

import com.certimakers.diagnosis.domain.model.ChecklistItem;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ReadinessScore;
import com.certimakers.diagnosis.domain.model.Requirement;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();
    private final ScoreRubric rubric = ScoreRubric.defaultsOnly(); // REQUIRED=3, RECOMMENDED=1

    @Test
    @DisplayName("보유 서류 가중치 합 / 전체 가중치 합 × 100 을 반올림한다")
    void 가중치_기반_점수_산정() {
        // 필수 2종(3+3=6) + 권장 1종(1) = 총 7. 필수 1종만 보유 → 3/7 = 42.857 → 43
        List<RequiredDocument> required = List.of(
                new RequiredDocument(BIZ_LICENSE, Requirement.REQUIRED),
                new RequiredDocument(TEST_REPORT, Requirement.REQUIRED),
                new RequiredDocument(CIRCUIT_DIAGRAM, Requirement.RECOMMENDED));

        ScoreResult result = calculator.calculate(required, Set.of(BIZ_LICENSE), rubric);

        assertThat(result.score().applicable()).isTrue();
        assertThat(result.score().earnedWeight()).isEqualTo(3);
        assertThat(result.score().totalWeight()).isEqualTo(7);
        assertThat(result.score().percentage()).isEqualTo(43);
    }

    @Test
    @DisplayName("모든 서류 보유 → 100%")
    void 전부_보유하면_만점() {
        List<RequiredDocument> required = List.of(
                new RequiredDocument(BIZ_LICENSE, Requirement.REQUIRED),
                new RequiredDocument(TEST_REPORT, Requirement.RECOMMENDED));

        ScoreResult result = calculator.calculate(required, Set.of(BIZ_LICENSE, TEST_REPORT), rubric);

        assertThat(result.score().percentage()).isEqualTo(100);
    }

    @Test
    @DisplayName("요구 서류가 없으면 0%가 아니라 '산정 불가'다 (불변식 2)")
    void 요구_서류_없으면_산정_불가() {
        ScoreResult result = calculator.calculate(List.of(), Set.of(), rubric);

        assertThat(result.score().applicable()).isFalse();
        assertThat(result.score()).isEqualTo(ReadinessScore.notApplicable());
    }

    @Test
    @DisplayName("보완 우선순위 = 누락 서류를 가중치 내림차순으로 정렬한 것")
    void 보완_우선순위는_가중치_내림차순() {
        List<RequiredDocument> required = List.of(
                new RequiredDocument(CIRCUIT_DIAGRAM, Requirement.RECOMMENDED), // 가중치 1, 누락
                new RequiredDocument(BIZ_LICENSE, Requirement.REQUIRED),        // 가중치 3, 누락
                new RequiredDocument(TEST_REPORT, Requirement.REQUIRED));       // 가중치 3, 보유

        ScoreResult result = calculator.calculate(required, Set.of(TEST_REPORT), rubric);

        List<ChecklistItem> order = result.remediationOrder();
        // 누락은 BIZ_LICENSE(3), CIRCUIT_DIAGRAM(1). 가중치 높은 것 먼저.
        assertThat(order).extracting(ChecklistItem::documentCode)
                .containsExactly(BIZ_LICENSE, CIRCUIT_DIAGRAM);
    }

    @Test
    @DisplayName("명시적 가중치가 있으면 요구 강도 기본값 대신 그 값을 쓴다")
    void 기준표의_명시적_가중치_우선() {
        DocumentCode special = DocumentCode.of("SPECIAL_DOC");
        ScoreRubric weighted = new ScoreRubric(Set.of(special).stream()
                .collect(java.util.stream.Collectors.toMap(code -> code, code -> 10)));

        List<RequiredDocument> required =
                List.of(new RequiredDocument(special, Requirement.RECOMMENDED)); // 기본 1이지만 기준표는 10

        ScoreResult result = calculator.calculate(required, Set.of(special), weighted);

        assertThat(result.checklist().get(0).weight()).isEqualTo(10);
        assertThat(result.score().percentage()).isEqualTo(100);
    }

    @Test
    @DisplayName("'모름'으로 체크한 서류는 보유로 치지 않되 '없음'과 구분해 기록한다")
    void 모름은_없음과_구분된다() {
        List<RequiredDocument> required = List.of(
                new RequiredDocument(DocumentCode.of("TEST_REPORT"), Requirement.REQUIRED),
                new RequiredDocument(DocumentCode.of("BIZ_LICENSE"), Requirement.REQUIRED),
                new RequiredDocument(DocumentCode.of("CIRCUIT_DIAGRAM"), Requirement.REQUIRED));

        ScoreResult result = new ScoreCalculator().calculate(
                required,
                Set.of(DocumentCode.of("TEST_REPORT")),
                Set.of(DocumentCode.of("BIZ_LICENSE")),
                ScoreRubric.defaultsOnly());

        assertThat(result.absentCount()).as("만들어야 하는 서류").isEqualTo(1);
        assertThat(result.unknownCount()).as("확인해야 하는 서류").isEqualTo(1);
        assertThat(result.score().earnedWeight())
                .as("'모름'은 획득 가중치에 들어가지 않는다")
                .isLessThan(result.score().totalWeight());
    }

    @Test
    @DisplayName("보유와 모름에 동시에 체크된 모순 입력은 보유로 본다")
    void 모순입력은_보유가_우선() {
        List<RequiredDocument> required = List.of(
                new RequiredDocument(DocumentCode.of("TEST_REPORT"), Requirement.REQUIRED));

        ScoreResult result = new ScoreCalculator().calculate(
                required,
                Set.of(DocumentCode.of("TEST_REPORT")),
                Set.of(DocumentCode.of("TEST_REPORT")),
                ScoreRubric.defaultsOnly());

        assertThat(result.unknownCount()).isZero();
        assertThat(result.checklist().get(0).held()).isTrue();
    }
}
