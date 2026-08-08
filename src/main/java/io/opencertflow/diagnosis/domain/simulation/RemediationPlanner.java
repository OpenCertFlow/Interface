package io.opencertflow.diagnosis.domain.simulation;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.ReadinessScore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 목표 준비도에 도달하는 <b>최소 개수</b>의 서류 조합을 찾는다. 순수 함수다.
 *
 * <p>가중치가 큰 서류부터 고르는 그리디가 최적이다. 목표는 "가중치 합이 임계값을 넘도록 하되 개수를
 * 최소화"하는 것이고, 이 문제에서는 큰 것부터 담는 것이 항상 최적해가 된다 — k개로 도달 가능하다면
 * 가장 큰 k개의 합은 어떤 k개 조합의 합보다 작지 않기 때문이다. 배낭 문제로 오해해 DP를 끌어올
 * 필요가 없다.
 *
 * <p>준비도 점수는 획득 가중치에 대해 단조 증가하므로, 가중치 기준의 최적해가 곧 점수 기준의
 * 최적해다.
 */
public class RemediationPlanner {

    /**
     * @param scoreResult 원본 진단의 점수 산정 결과 (체크리스트 포함)
     * @param targetScore 목표 준비도(%). 1~100
     */
    public RemediationPlan planFor(ScoreResultView scoreResult, int targetScore) {
        // 웹 계층이 아니라 여기서 막는다. 어떤 경로로 들어오든 목표 범위는 도메인이 보장한다.
        Guard.inRange(targetScore, 1, 100, "targetScore");
        ReadinessScore score = scoreResult.score();

        // 불변식 2: 요구 서류가 없으면 점수 자체가 없다. 목표를 논할 수 없다.
        if (!score.applicable()) {
            return RemediationPlan.notApplicable(targetScore);
        }
        if (score.percentage() >= targetScore) {
            return RemediationPlan.alreadyMet(score.percentage(), targetScore);
        }

        int total = score.totalWeight();
        int earned = score.earnedWeight();

        // 가중치 내림차순, 동률이면 서류 코드순으로 고정해 결과를 재현 가능하게 한다.
        List<ChecklistItem> missing = scoreResult.checklist().stream()
                .filter(ChecklistItem::isMissing)
                .sorted(Comparator.comparingInt(ChecklistItem::weight).reversed()
                        .thenComparing(item -> item.documentCode().value()))
                .toList();

        List<RemediationStep> steps = new ArrayList<>();
        int runningEarned = earned;
        int previousScore = score.percentage();

        for (ChecklistItem item : missing) {
            if (previousScore >= targetScore) {
                break;
            }
            runningEarned += item.weight();
            int scoreAfter = percentageOf(runningEarned, total);
            steps.add(new RemediationStep(
                    steps.size() + 1,
                    item.documentCode(),
                    item.requirement(),
                    item.weight(),
                    scoreAfter,
                    scoreAfter - previousScore));
            previousScore = scoreAfter;
        }

        int projected = previousScore;
        return new RemediationPlan(
                score.percentage(),
                targetScore,
                true,
                projected >= targetScore,
                projected,
                steps,
                missing.size() - steps.size());
    }

    /** {@link ReadinessScore#of}와 같은 반올림 규칙을 쓴다. 두 곳의 점수가 어긋나면 안 된다. */
    private int percentageOf(int earned, int total) {
        return Math.round((float) earned / total * 100);
    }

    /**
     * 계획 수립에 필요한 최소 입력. {@code ScoreResult}와 진단 애그리거트 양쪽에서 만들 수 있도록
     * 좁은 뷰로 받는다 — 저장된 진단으로 계획을 세울 때 애그리거트를 점수 서비스 타입으로 억지로
     * 되돌릴 필요가 없다.
     */
    public record ScoreResultView(ReadinessScore score, List<ChecklistItem> checklist) {
        public ScoreResultView {
            checklist = List.copyOf(checklist);
        }
    }
}
