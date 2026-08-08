package io.opencertflow.diagnosis.domain.service;

import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.ChecklistStatus;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.ReadinessScore;
import java.util.List;
import java.util.Set;

/**
 * 준비도 점수 산정 도메인 서비스. {@link RuleEvaluator}와 마찬가지로 순수 함수다.
 *
 * <p>점수 = round(보유한 요구 서류의 가중치 합 / 전체 요구 서류의 가중치 합 × 100).
 * 요구 서류가 없으면 0%가 아니라 <b>산정 불가</b>다(불변식 2, {@link ReadinessScore}).
 *
 * <p>'모름'으로 체크한 서류는 보유로 치지 않는다. 다만 '없음'과도 구분해 기록한다 —
 * 미확인을 정상으로도 부적합으로도 해석하지 않는 것이 서비스 원칙이고(운영지침 §9),
 * 사용자가 할 다음 행동이 다르기 때문이다.
 */
public class ScoreCalculator {

    public ScoreResult calculate(
            List<RequiredDocument> requiredDocuments,
            Set<DocumentCode> heldDocuments,
            ScoreRubric rubric) {
        return calculate(requiredDocuments, heldDocuments, Set.of(), rubric);
    }

    public ScoreResult calculate(
            List<RequiredDocument> requiredDocuments,
            Set<DocumentCode> heldDocuments,
            Set<DocumentCode> unknownDocuments,
            ScoreRubric rubric) {

        List<ChecklistItem> checklist = requiredDocuments.stream()
                .map(required ->
                        toChecklistItem(required, heldDocuments, unknownDocuments, rubric))
                .toList();

        int totalWeight = checklist.stream().mapToInt(ChecklistItem::weight).sum();
        int earnedWeight = checklist.stream()
                .filter(ChecklistItem::held)
                .mapToInt(ChecklistItem::weight)
                .sum();

        ReadinessScore score = ReadinessScore.of(earnedWeight, totalWeight);
        return new ScoreResult(score, checklist);
    }

    private ChecklistItem toChecklistItem(
            RequiredDocument required,
            Set<DocumentCode> held,
            Set<DocumentCode> unknown,
            ScoreRubric rubric) {

        int weight = rubric.weightOf(required.documentCode(), required.requirement());
        return new ChecklistItem(
                required.documentCode(), required.requirement(), weight,
                statusOf(required.documentCode(), held, unknown));
    }

    /** 보유가 '모름'을 이긴다 — 양쪽에 체크된 모순 입력은 보유로 본다. */
    private ChecklistStatus statusOf(
            DocumentCode code, Set<DocumentCode> held, Set<DocumentCode> unknown) {
        if (held.contains(code)) {
            return ChecklistStatus.HELD;
        }
        return unknown.contains(code) ? ChecklistStatus.UNKNOWN : ChecklistStatus.MISSING;
    }
}
