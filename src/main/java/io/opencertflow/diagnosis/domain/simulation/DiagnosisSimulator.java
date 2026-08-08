package io.opencertflow.diagnosis.domain.simulation;

import io.opencertflow.diagnosis.domain.model.CertificationCandidate;
import io.opencertflow.diagnosis.domain.model.CertificationType;
import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.ProductProfile;
import io.opencertflow.diagnosis.domain.model.ReadinessScore;
import io.opencertflow.diagnosis.domain.model.SchemeCode;
import io.opencertflow.diagnosis.domain.rule.RuleSet;
import io.opencertflow.diagnosis.domain.service.RequiredDocument;
import io.opencertflow.diagnosis.domain.service.RuleEvaluationResult;
import io.opencertflow.diagnosis.domain.service.RuleEvaluator;
import io.opencertflow.diagnosis.domain.service.ScoreCalculator;
import io.opencertflow.diagnosis.domain.service.ScoreResult;
import io.opencertflow.diagnosis.domain.service.ScoreRubric;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 반사실(what-if) 시뮬레이터. "이 서류를 준비하면", "배터리를 빼면" 결과가 어떻게 달라지는지
 * <b>정확한 수치로</b> 답한다.
 *
 * <p>이 기능이 성립하는 이유는 진단이 결정론적 룰 엔진이기 때문이다(ADR-0003). 같은 입력이 항상
 * 같은 결과를 내므로, 입력 하나를 바꾼 결과 역시 확정적으로 계산된다. 판정을 LLM에 맡겼다면
 * "아마 오를 것"이라는 추측밖에 할 수 없다 — 시뮬레이션은 결정론 선택이 만들어 낸 직접적 산물이다.
 *
 * <p>{@link RuleEvaluator}·{@link ScoreCalculator}와 마찬가지로 <b>순수 함수</b>다. 스프링 빈이 아니고
 * 상태가 없으며, 원본 진단을 일절 변경하지 않는다.
 */
public class DiagnosisSimulator {

    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();
    private final ScoreCalculator scoreCalculator = new ScoreCalculator();

    /**
     * 가정을 적용해 다시 평가하고, 원본과의 차이를 계산한다.
     *
     * <p>원본은 이미 확정된 기록이므로 다시 평가하지 않는다. 저장된 점수·체크리스트·후보를 그대로
     * 비교 기준으로 삼는다 — 원본을 재평가하면 그 사이 룰셋이 바뀌었을 때 "가정 때문에 달라진 것"과
     * "룰이 바뀌어서 달라진 것"이 뒤섞인다.
     *
     * @param baseProfile    원본 진단의 제품 프로파일
     * @param baseScore      원본 진단의 준비도 점수
     * @param baseChecklist  원본 진단의 체크리스트 (요구 서류 스냅샷)
     * @param baseCandidates 원본 진단의 인증 후보
     * @param adjustment     적용할 가정
     * @param ruleSet        평가에 쓸 룰셋. 원본과 같은 버전을 넘겨야 비교가 성립한다
     * @param rubric         점수 가중치 기준표
     */
    public SimulationOutcome simulate(
            ProductProfile baseProfile,
            ReadinessScore baseScore,
            List<ChecklistItem> baseChecklist,
            List<CertificationCandidate> baseCandidates,
            ProfileAdjustment adjustment,
            RuleSet ruleSet,
            ScoreRubric rubric) {

        ProductProfile adjusted = adjustment.applyTo(baseProfile);

        RuleEvaluationResult ruleResult = ruleEvaluator.evaluate(adjusted, ruleSet);
        ScoreResult scoreResult = scoreCalculator.calculate(
                ruleResult.requiredDocuments(), adjusted.heldDocuments(), rubric);

        Set<CandidateKey> baseCandidateKeys = toKeys(baseCandidates);
        Set<CandidateKey> adjustedCandidateKeys = toKeys(ruleResult.candidates());

        Set<DocumentCode> baseRequired = baseChecklist.stream()
                .map(ChecklistItem::documentCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<DocumentCode> adjustedRequired = ruleResult.requiredDocuments().stream()
                .map(RequiredDocument::documentCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new SimulationOutcome(
                adjusted,
                ruleResult,
                scoreResult,
                new ScoreDelta(baseScore, scoreResult.score()),
                ruleResult.candidates().stream()
                        .filter(candidate -> !baseCandidateKeys.contains(CandidateKey.of(candidate)))
                        .toList(),
                baseCandidates.stream()
                        .filter(candidate -> !adjustedCandidateKeys.contains(CandidateKey.of(candidate)))
                        .toList(),
                sortedDifference(adjustedRequired, baseRequired),
                sortedDifference(baseRequired, adjustedRequired),
                sortedDifference(adjusted.heldDocuments(), baseProfile.heldDocuments()));
    }

    private Set<CandidateKey> toKeys(List<CertificationCandidate> candidates) {
        return candidates.stream().map(CandidateKey::of).collect(Collectors.toUnmodifiableSet());
    }

    /** 좌변에만 있는 서류를 코드 사전순으로. 결과 순서까지 재현 가능해야 한다. */
    private List<DocumentCode> sortedDifference(Set<DocumentCode> left, Set<DocumentCode> right) {
        return left.stream()
                .filter(code -> !right.contains(code))
                .sorted(Comparator.comparing(DocumentCode::value))
                .toList();
    }

    /** 후보 동일성 판단 기준. (제도, 인증 유형)이 같으면 같은 후보다. */
    private record CandidateKey(SchemeCode schemeCode, CertificationType type) {
        static CandidateKey of(CertificationCandidate candidate) {
            return new CandidateKey(candidate.schemeCode(), candidate.type());
        }
    }
}
