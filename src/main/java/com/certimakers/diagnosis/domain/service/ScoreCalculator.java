package com.certimakers.diagnosis.domain.service;

import com.certimakers.diagnosis.domain.model.ChecklistItem;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ReadinessScore;
import java.util.List;
import java.util.Set;

/**
 * 준비도 점수 산정 도메인 서비스. {@link RuleEvaluator}와 마찬가지로 순수 함수다.
 *
 * <p>점수 = round(보유한 요구 서류의 가중치 합 / 전체 요구 서류의 가중치 합 × 100).
 * 요구 서류가 없으면 0%가 아니라 <b>산정 불가</b>다(불변식 2, {@link ReadinessScore}).
 */
public class ScoreCalculator {

    public ScoreResult calculate(
            List<RequiredDocument> requiredDocuments,
            Set<DocumentCode> heldDocuments,
            ScoreRubric rubric) {

        List<ChecklistItem> checklist = requiredDocuments.stream()
                .map(required -> toChecklistItem(required, heldDocuments, rubric))
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
            RequiredDocument required, Set<DocumentCode> held, ScoreRubric rubric) {

        int weight = rubric.weightOf(required.documentCode(), required.requirement());
        boolean isHeld = held.contains(required.documentCode());
        return new ChecklistItem(required.documentCode(), required.requirement(), weight, isHeld);
    }
}
