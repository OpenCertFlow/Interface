package io.opencertflow.diagnosis.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.diagnosis.domain.ProductProfileFixtures;
import io.opencertflow.diagnosis.domain.RuleSetFixtures;
import io.opencertflow.diagnosis.domain.service.RuleEvaluationResult;
import io.opencertflow.diagnosis.domain.service.RuleEvaluator;
import io.opencertflow.diagnosis.domain.service.ScoreCalculator;
import io.opencertflow.diagnosis.domain.service.ScoreResult;
import io.opencertflow.diagnosis.domain.service.ScoreRubric;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiagnosisTest {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();
    private final ScoreCalculator scoreCalculator = new ScoreCalculator();

    private Diagnosis evaluatedDryer() {
        ProductProfile dryer = ProductProfileFixtures.hairDryer(Set.of(RuleSetFixtures.TEST_REPORT));
        Diagnosis diagnosis =
                Diagnosis.request(
                        DiagnosisId.of(io.opencertflow.support.TestIds.next()), dryer, null, null, NOW);

        RuleEvaluationResult ruleResult =
                ruleEvaluator.evaluate(dryer, RuleSetFixtures.smallApplianceV1());
        ScoreResult scoreResult = scoreCalculator.calculate(
                ruleResult.requiredDocuments(), dryer.heldDocuments(), ScoreRubric.defaultsOnly());

        diagnosis.evaluated(ruleResult, scoreResult);
        return diagnosis;
    }

    @Test
    @DisplayName("요청 상태로 시작한다")
    void 요청_상태로_시작() {
        Diagnosis diagnosis = Diagnosis.request(
                DiagnosisId.of(io.opencertflow.support.TestIds.next()),
                ProductProfileFixtures.hairDryer(Set.of()),
                null,
                null,
                NOW);

        assertThat(diagnosis.status()).isEqualTo(DiagnosisStatus.REQUESTED);
    }

    @Test
    @DisplayName("평가하면 RULE_EVALUATED로 전이하고 판정·점수를 담는다")
    void 평가하면_판정과_점수_확정() {
        Diagnosis diagnosis = evaluatedDryer();

        assertThat(diagnosis.status()).isEqualTo(DiagnosisStatus.RULE_EVALUATED);
        assertThat(diagnosis.ruleSetVersion().value()).isEqualTo(1);
        assertThat(diagnosis.candidates()).isNotEmpty();
        assertThat(diagnosis.score().applicable()).isTrue();
    }

    @Test
    @DisplayName("RULE_EVALUATED 전에는 근거를 첨부할 수 없다 (불변식 4)")
    void 평가전_근거첨부_불가() {
        Diagnosis diagnosis = Diagnosis.request(
                DiagnosisId.of(io.opencertflow.support.TestIds.next()),
                ProductProfileFixtures.hairDryer(Set.of()),
                null,
                null,
                NOW);

        assertThatThrownBy(() -> diagnosis.attachEvidences(List.of()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("근거·문장화 성공 시 COMPLETED")
    void 온전히_완료되면_COMPLETED() {
        Diagnosis diagnosis = evaluatedDryer();

        diagnosis.attachEvidences(List.of(new Evidence(
                "doc-1", "DOCUMENTS", "안전확인 대상 전기용품은...", URI.create("https://example.kr/doc-1"), 0.82)));
        diagnosis.attachNarration(new Narration(
                "220V 드라이기는 안전확인 대상으로 보입니다.",
                List.of("시험기관에 시험성적서를 신청하세요."),
                List.of("정격전압이 정확히 몇 V인가요?"),
                "본 결과는 사전 점검 지표이며 인증 합격을 보장하지 않습니다.",
                "claude-opus-4-8",
                false));
        diagnosis.complete();

        assertThat(diagnosis.status()).isEqualTo(DiagnosisStatus.COMPLETED);
        assertThat(diagnosis.degraded().any()).isFalse();
    }

    @Test
    @DisplayName("근거 조회 실패 시 COMPLETED_DEGRADED — 판정과 점수는 그대로 유효하다")
    void 근거_실패시_저하완료() {
        Diagnosis diagnosis = evaluatedDryer();
        ReadinessScore scoreBefore = diagnosis.score();

        diagnosis.markEvidenceDegraded();
        diagnosis.attachNarration(templateNarration());
        diagnosis.complete();

        assertThat(diagnosis.status()).isEqualTo(DiagnosisStatus.COMPLETED_DEGRADED);
        assertThat(diagnosis.degraded().isEvidenceDegraded()).isTrue();
        // 저하되어도 점수는 바뀌지 않는다
        assertThat(diagnosis.score()).isEqualTo(scoreBefore);
    }

    @Test
    @DisplayName("템플릿 폴백 문장을 붙이면 narration degraded로 표시된다")
    void 템플릿_폴백은_저하로_표시() {
        Diagnosis diagnosis = evaluatedDryer();

        diagnosis.attachNarration(templateNarration());
        diagnosis.complete();

        assertThat(diagnosis.status()).isEqualTo(DiagnosisStatus.COMPLETED_DEGRADED);
        assertThat(diagnosis.degraded().isNarrationDegraded()).isTrue();
    }

    @Test
    @DisplayName("두 번 완료 처리하면 상태 전이 규칙 위반으로 거부한다")
    void 중복_완료_거부() {
        Diagnosis diagnosis = evaluatedDryer();
        diagnosis.attachNarration(templateNarration());
        diagnosis.complete();

        assertThatThrownBy(diagnosis::complete).isInstanceOf(BusinessException.class);
    }

    private Narration templateNarration() {
        return new Narration(
                "규칙 기반 요약입니다.",
                List.of("누락 서류를 준비하세요."),
                List.of(),
                "본 결과는 사전 점검 지표입니다.",
                "template",
                true);
    }

    // ── 열람 권한 ────────────────────────────────────────────────
    //
    // 식별자가 전역 시퀀스라 1, 2, 3…으로 열거된다. 소유자 검증이 없으면 로그인 사용자의
    // 제품 사양·제조 방식이 그대로 새어 나가므로, 이 판정은 도메인에서 못 박는다.

    private Diagnosis ownedBy(String ownerUserId) {
        return Diagnosis.request(
                DiagnosisId.of(io.opencertflow.support.TestIds.next()),
                ProductProfileFixtures.hairDryer(Set.of()),
                ownerUserId,
                null,
                NOW);
    }

    @Test
    @DisplayName("소유자가 있는 진단은 본인만 볼 수 있다")
    void 소유자_본인만_열람() {
        Diagnosis diagnosis = ownedBy("42");

        assertThat(diagnosis.isVisibleTo("42")).isTrue();
    }

    @Test
    @DisplayName("소유자가 있는 진단은 다른 사용자가 볼 수 없다")
    void 타인은_열람_불가() {
        Diagnosis diagnosis = ownedBy("42");

        assertThat(diagnosis.isVisibleTo("43")).isFalse();
    }

    @Test
    @DisplayName("소유자가 있는 진단은 비로그인으로 볼 수 없다")
    void 비로그인은_소유_진단_열람_불가() {
        Diagnosis diagnosis = ownedBy("42");

        assertThat(diagnosis.isVisibleTo(null)).isFalse();
    }

    @Test
    @DisplayName("익명 진단은 누구나 볼 수 있다 — 비로그인 진단 흐름이 성립해야 한다")
    void 익명_진단은_공개() {
        Diagnosis diagnosis = ownedBy(null);

        assertThat(diagnosis.isVisibleTo(null)).isTrue();
        assertThat(diagnosis.isVisibleTo("42")).isTrue();
    }
}
