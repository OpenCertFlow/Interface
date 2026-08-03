package com.certimakers.diagnosis.domain.service;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.ChecklistItem;
import com.certimakers.diagnosis.domain.model.ReadinessScore;
import java.util.Comparator;
import java.util.List;

/**
 * 점수 산정 결과. 준비도 점수와 완성된 체크리스트, 그리고 보완 우선순위를 담는다.
 *
 * @param score      준비도 점수 (산정 불가일 수 있음)
 * @param checklist  가중치·보유 여부가 결합된 체크리스트 전체
 */
public record ScoreResult(ReadinessScore score, List<ChecklistItem> checklist) {

    public ScoreResult {
        Guard.notNull(score, "score");
        checklist = List.copyOf(Guard.notNull(checklist, "checklist"));
    }

    /**
     * 보완 우선순위 = 누락 서류를 가중치 내림차순으로 정렬한 것.
     *
     * <p>별도 알고리즘이 아니다. 점수를 가장 많이 올리는 순서가 곧 우선순위다(03-diagnosis-flow.md).
     * 동률이면 서류 코드순으로 고정해 결과를 재현 가능하게 한다.
     */
    public List<ChecklistItem> remediationOrder() {
        return checklist.stream()
                .filter(ChecklistItem::isMissing)
                .sorted(Comparator.comparingInt(ChecklistItem::weight).reversed()
                        .thenComparing(item -> item.documentCode().value()))
                .toList();
    }

    /** 사용자가 만들어야 하는 서류 수. 리포트의 '누락자료 n건'. */
    public long absentCount() {
        return checklist.stream().filter(ChecklistItem::isAbsent).count();
    }

    /** 사용자가 확인해야 하는 서류 수. 리포트의 '확인 중 n건'. */
    public long unknownCount() {
        return checklist.stream().filter(ChecklistItem::isUnknown).count();
    }
}
