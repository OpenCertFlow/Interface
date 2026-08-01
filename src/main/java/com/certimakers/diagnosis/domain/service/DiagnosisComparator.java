package com.certimakers.diagnosis.domain.service;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.error.DiagnosisErrorCode;
import com.certimakers.diagnosis.domain.model.ChecklistItem;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.simulation.ScoreDelta;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 원 진단과 재진단을 비교한다(F-APP-048).
 *
 * <p>두 애그리거트에 걸친 계산이라 어느 진단의 메서드도 될 수 없어 도메인 서비스로 둔다.
 * 상태 없는 순수 함수이므로 스프링 빈이 아니며, {@code RuleEvaluator}·{@code ScoreCalculator}처럼
 * 애플리케이션 서비스가 직접 생성해 보유한다(도메인이 스프링을 참조하면 ArchUnit이 막는다).
 */
public class DiagnosisComparator {

    /**
     * 두 진단을 비교한다. {@code current}가 {@code previous}의 재진단임은 호출부가 보장한다
     * (previous_id를 따라 꺼내 왔으므로).
     */
    public DiagnosisComparison compare(Diagnosis previous, Diagnosis current) {
        Guard.notNull(previous, "previous");
        Guard.notNull(current, "current");

        // 룰 평가 전 진단은 점수·체크리스트가 없어 비교 기준이 없다.
        if (previous.score() == null || current.score() == null) {
            throw new BusinessException(DiagnosisErrorCode.NOT_COMPARABLE);
        }

        // 제품군 검증(정의서): 제품군이 다르면 요구 서류 집합 자체가 달라 비교가 성립하지 않는다.
        // 재진단은 입력을 복사하므로 실제로는 항상 같지만, 정의서에 명시된 거부 조건이라 둔다.
        if (previous.profile().productGroup() != current.profile().productGroup()) {
            throw new BusinessException(DiagnosisErrorCode.NOT_COMPARABLE);
        }

        // 원본에서 '없던' 서류들. 이 집합에 있던 게 지금 held=true면 새로 갖춘 것이다.
        Set<DocumentCode> previouslyMissing = previous.checklist().stream()
                .filter(ChecklistItem::isMissing)
                .map(ChecklistItem::documentCode)
                .collect(Collectors.toSet());

        List<DocumentCode> newlyHeld = current.checklist().stream()
                .filter(item -> item.held() && previouslyMissing.contains(item.documentCode()))
                .map(ChecklistItem::documentCode)
                .sorted(Comparator.comparing(DocumentCode::value))   // 응답 순서 고정(테스트 안정)
                .toList();

        List<DocumentCode> stillMissing = current.checklist().stream()
                .filter(ChecklistItem::isMissing)
                .map(ChecklistItem::documentCode)
                .sorted(Comparator.comparing(DocumentCode::value))
                .toList();

        // 기준 차이 = 룰셋 버전 차이 또는 가중치 기준표 변경(정의서: "Rule 또는 점수버전").
        // ruleSetVersion은 null일 수 있어 Objects.equals로 비교한다(NPE 회피).
        boolean ruleSetDiffers =
                !Objects.equals(previous.ruleSetVersion(), current.ruleSetVersion());
        boolean baselineDiffers = ruleSetDiffers || weightsChanged(previous, current);

        return new DiagnosisComparison(
                previous.id(),
                current.id(),
                new ScoreDelta(previous.score(), current.score()),  // before=원본, after=재진단
                newlyHeld,
                stillMissing,
                baselineDiffers);
    }

    /**
     * 같은 서류의 가중치가 두 진단에서 다르면 기준표(ScoreRubric)가 바뀐 것이다.
     *
     * <p>{@code ScoreRubric}에는 버전이 없지만 {@link ChecklistItem#weight()}가 평가 시점의
     * 스냅샷이라, 값을 맞대보면 컬럼 추가 없이 기준표 변경을 감지할 수 있다.
     */
    private boolean weightsChanged(Diagnosis previous, Diagnosis current) {
        Map<DocumentCode, Integer> previousWeights = previous.checklist().stream()
                .collect(Collectors.toMap(ChecklistItem::documentCode, ChecklistItem::weight));
        return current.checklist().stream().anyMatch(item -> {
            Integer before = previousWeights.get(item.documentCode());
            return before != null && before != item.weight();  // 양쪽에 있는 서류만 비교
        });
    }
}
