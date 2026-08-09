package io.opencertflow.diagnosis.domain.service;

import static io.opencertflow.diagnosis.domain.RuleSetFixtures.BIZ_LICENSE;
import static io.opencertflow.diagnosis.domain.RuleSetFixtures.CIRCUIT_DIAGRAM;
import static io.opencertflow.diagnosis.domain.RuleSetFixtures.TEST_REPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.diagnosis.domain.ProductProfileFixtures;
import io.opencertflow.diagnosis.domain.model.PowerSource;
import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.DegradedFlags;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.DiagnosisStatus;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.ElectricalSpec;
import io.opencertflow.diagnosis.domain.model.MaterialType;
import io.opencertflow.diagnosis.domain.model.ProductGroup;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import io.opencertflow.diagnosis.domain.model.ReadinessScore;
import io.opencertflow.diagnosis.domain.model.Requirement;
import io.opencertflow.diagnosis.domain.model.SalesChannel;
import io.opencertflow.diagnosis.domain.model.TargetUser;
import io.opencertflow.diagnosis.domain.rule.RuleSetVersion;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 재진단 비교 계산 검증(F-APP-048). 목 없이 순수 객체로 돌린다.
 *
 * <p>진단은 {@code request()} 대신 {@code reconstitute()}로 만든다. {@code request()}는 상태를
 * REQUESTED로 시작하므로 체크리스트·점수를 채우려면 룰 평가를 통째로 돌려야 하고, 그러면 룰이
 * 바뀔 때마다 이 테스트가 깨진다. 여기서 검증할 것은 비교 계산이지 룰 평가가 아니다.
 */
class DiagnosisComparatorTest {

    private final DiagnosisComparator comparator = new DiagnosisComparator();

    @Test
    @DisplayName("원본에서 없던 서류를 재진단에서 갖췄으면 신규 충족으로 잡힌다")
    void 신규_충족_서류를_찾는다() {
        Diagnosis previous = diagnosis(42, 1, score(40, 4, 10), List.of(
                item(TEST_REPORT, 5, false),        // 없었음
                item(CIRCUIT_DIAGRAM, 5, false)));  // 없었음
        Diagnosis current = diagnosis(57, 1, score(70, 7, 10), List.of(
                item(TEST_REPORT, 5, true),         // ← 이번에 갖춤
                item(CIRCUIT_DIAGRAM, 5, false)));  // 아직 없음

        DiagnosisComparison result = comparator.compare(previous, current);

        assertThat(result.previousId()).isEqualTo(DiagnosisId.of(42L));
        assertThat(result.currentId()).isEqualTo(DiagnosisId.of(57L));
        assertThat(result.newlyHeld()).containsExactly(TEST_REPORT);
        assertThat(result.stillMissing()).containsExactly(CIRCUIT_DIAGRAM);
        assertThat(result.delta().percentagePointChange()).isEqualTo(30);
        assertThat(result.delta().improved()).isTrue();
    }

    @Test
    @DisplayName("원본에서 이미 갖고 있던 서류는 신규 충족이 아니다")
    void 원래_갖고_있던_서류는_제외된다() {
        Diagnosis previous = diagnosis(42, 1, score(50, 5, 10), List.of(item(BIZ_LICENSE, 5, true)));
        Diagnosis current = diagnosis(57, 1, score(50, 5, 10), List.of(item(BIZ_LICENSE, 5, true)));

        DiagnosisComparison result = comparator.compare(previous, current);

        assertThat(result.newlyHeld()).isEmpty();
        assertThat(result.stillMissing()).isEmpty();
        assertThat(result.delta().percentagePointChange()).isZero();
    }

    @Test
    @DisplayName("신규 충족·잔여 서류는 코드순으로 정렬해 응답 순서를 고정한다")
    void 서류_목록은_코드순으로_정렬된다() {
        Diagnosis previous = diagnosis(42, 1, score(0, 0, 15), List.of(
                item(TEST_REPORT, 5, false),
                item(BIZ_LICENSE, 5, false),
                item(CIRCUIT_DIAGRAM, 5, false)));
        Diagnosis current = diagnosis(57, 1, score(67, 10, 15), List.of(
                item(TEST_REPORT, 5, true),
                item(BIZ_LICENSE, 5, true),
                item(CIRCUIT_DIAGRAM, 5, false)));

        DiagnosisComparison result = comparator.compare(previous, current);

        // 체크리스트 순서(TEST_REPORT 먼저)와 무관하게 코드 알파벳순으로 나온다.
        assertThat(result.newlyHeld()).containsExactly(BIZ_LICENSE, TEST_REPORT);
    }

    @Test
    @DisplayName("룰셋 버전이 다르면 기준 차이를 표시한다 — 점수 차를 개선으로 단정할 수 없다")
    void 룰셋_버전이_다르면_기준_차이를_표시한다() {
        Diagnosis previous = diagnosis(42, 1, score(40, 4, 10), List.of(item(TEST_REPORT, 5, false)));
        Diagnosis current = diagnosis(57, 2, score(60, 6, 10), List.of(item(TEST_REPORT, 5, false)));
        //                                ↑ 룰셋 v2

        assertThat(comparator.compare(previous, current).baselineDiffers()).isTrue();
    }

    @Test
    @DisplayName("룰셋 버전이 같고 가중치도 그대로면 기준 차이가 없다")
    void 기준이_그대로면_차이가_없다() {
        Diagnosis previous = diagnosis(42, 1, score(40, 4, 10), List.of(item(TEST_REPORT, 5, false)));
        Diagnosis current = diagnosis(57, 1, score(40, 4, 10), List.of(item(TEST_REPORT, 5, false)));

        assertThat(comparator.compare(previous, current).baselineDiffers()).isFalse();
    }

    @Test
    @DisplayName("룰셋 버전이 같아도 같은 서류의 가중치가 바뀌면 기준 차이로 본다 (점수버전 변경)")
    void 가중치가_바뀌면_기준_차이를_표시한다() {
        Diagnosis previous = diagnosis(42, 1, score(40, 4, 10), List.of(item(TEST_REPORT, 3, false)));
        Diagnosis current = diagnosis(57, 1, score(40, 4, 10), List.of(item(TEST_REPORT, 5, false)));
        //                                                                            ↑ 3 → 5

        assertThat(comparator.compare(previous, current).baselineDiffers()).isTrue();
    }

    @Test
    @DisplayName("한쪽 요구 서류 목록에만 있는 서류는 가중치 비교에서 제외한다")
    void 한쪽에만_있는_서류는_가중치_비교에서_빠진다() {
        Diagnosis previous = diagnosis(42, 1, score(0, 0, 5), List.of(item(TEST_REPORT, 5, false)));
        Diagnosis current = diagnosis(57, 1, score(0, 0, 6), List.of(
                item(TEST_REPORT, 5, false),
                item(CIRCUIT_DIAGRAM, 1, false)));  // 42엔 없던 서류. 가중치 변경이 아니다

        assertThat(comparator.compare(previous, current).baselineDiffers()).isFalse();
        assertThat(comparator.compare(previous, current).stillMissing())
                .containsExactly(CIRCUIT_DIAGRAM, TEST_REPORT);   // 요구 서류 변화는 여기 드러난다
    }

    @Test
    @DisplayName("한쪽 점수가 산정 불가면 비교 불가로 표시하고 변화량을 쓰지 않는다 (불변식 2)")
    void 산정_불가면_변화량을_쓰지_않는다() {
        Diagnosis previous = diagnosis(42, 1, ReadinessScore.notApplicable(), List.of());
        Diagnosis current = diagnosis(57, 1, score(70, 7, 10), List.of());

        DiagnosisComparison result = comparator.compare(previous, current);

        assertThat(result.delta().comparable()).isFalse();
        // 0이 나오지만 '변화 없음'으로 읽으면 안 된다 — comparable=false로 구분한다.
        assertThat(result.delta().percentagePointChange()).isZero();
    }

    @Test
    @DisplayName("룰 평가 전 진단(점수 없음)은 비교 기준이 없어 거부한다")
    void 평가_전_진단은_비교할_수_없다() {
        Diagnosis previous = diagnosis(42, 1, null, List.of());   // score = null
        Diagnosis current = diagnosis(57, 1, score(70, 7, 10), List.of());

        assertThatThrownBy(() -> comparator.compare(previous, current))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("제품군이 다르면 요구 서류 집합 자체가 달라 거부한다 (정의서 거부 조건)")
    void 제품군이_다르면_비교할_수_없다() {
        Diagnosis previous = diagnosis(42, 1, score(40, 4, 10), List.of());          // 헤어드라이어
        Diagnosis current = heatingPadDiagnosis(57, 1, score(70, 7, 10), List.of()); // 전기방석

        assertThatThrownBy(() -> comparator.compare(previous, current))
                .isInstanceOf(BusinessException.class);
    }

    // ── 픽스처 ────────────────────────────────────────────────────

    /** 소형가전(헤어드라이어) 진단. */
    private Diagnosis diagnosis(
            long id, int ruleSetVersion, ReadinessScore score, List<ChecklistItem> checklist) {
        return reconstitute(id, ProductProfileFixtures.hairDryer(Set.of()), ruleSetVersion, score, checklist);
    }

    /** 전기방석 진단. 제품군 불일치 검증용. */
    private Diagnosis heatingPadDiagnosis(
            long id, int ruleSetVersion, ReadinessScore score, List<ChecklistItem> checklist) {
        ProductProfile heatingPad = new ProductProfile(
                "보온용 전기방석",
                ProductGroup.ELECTRIC_HEATING_PAD,
                new ElectricalSpec(true, 220, 60, false, PowerSource.AC),
                TargetUser.GENERAL,
                SalesChannel.ONLINE,
                Set.of(MaterialType.TEXTILE),
                Set.of());
        return reconstitute(id, heatingPad, ruleSetVersion, score, checklist);
    }

    private Diagnosis reconstitute(
            long id, ProductProfile profile, int ruleSetVersion,
            ReadinessScore score, List<ChecklistItem> checklist) {
        return Diagnosis.reconstitute(
                DiagnosisId.of(id),
                profile,
                "owner-1",
                null,                       // 부모 참조는 비교 계산에 쓰이지 않는다(호출부가 따라온다)
                Instant.EPOCH,
                DiagnosisStatus.COMPLETED,
                RuleSetVersion.of(ruleSetVersion),
                score,
                List.of(), checklist, List.of(), List.of(), List.of(),
                null,
                DegradedFlags.of(false, false));
    }

    private ChecklistItem item(DocumentCode code, int weight, boolean held) {
        return new ChecklistItem(code, Requirement.REQUIRED, weight, held);
    }

    private ReadinessScore score(int percentage, int earned, int total) {
        return new ReadinessScore(true, percentage, earned, total);
    }
}
