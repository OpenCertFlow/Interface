package com.certimakers.diagnosis.domain.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.domain.model.ChecklistItem;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ReadinessScore;
import com.certimakers.diagnosis.domain.model.Requirement;
import com.certimakers.diagnosis.domain.simulation.RemediationPlanner.ScoreResultView;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 최소 보완 경로 계산. 순수 함수이므로 스프링 컨텍스트 없이 밀리초 안에 끝난다.
 *
 * <p>기준 시나리오: 요구 서류 5종(가중치 3·3·3·1·1, 총합 11) 중 BIZ_LICENSE(3)만 보유 → 27%.
 */
class RemediationPlannerTest {

    private final RemediationPlanner planner = new RemediationPlanner();

    private static ChecklistItem item(String code, Requirement requirement, int weight, boolean held) {
        return new ChecklistItem(DocumentCode.of(code), requirement, weight, held);
    }

    /** 3+3+3+1+1 = 11 중 BIZ_LICENSE(3)만 보유 → 27%. */
    private static ScoreResultView baseline() {
        List<ChecklistItem> checklist = List.of(
                item("BIZ_LICENSE", Requirement.REQUIRED, 3, true),
                item("TEST_REPORT", Requirement.REQUIRED, 3, false),
                item("SAFETY_LABEL_SAMPLE", Requirement.REQUIRED, 3, false),
                item("CIRCUIT_DIAGRAM", Requirement.RECOMMENDED, 1, false),
                item("PARTS_LIST", Requirement.RECOMMENDED, 1, false));
        return new ScoreResultView(ReadinessScore.of(3, 11), checklist);
    }

    @Nested
    @DisplayName("최소 개수로 목표에 도달한다")
    class MinimalPath {

        @Test
        @DisplayName("가중치가 큰 서류부터 골라 목표 직후에 멈춘다")
        void 가중치_큰_서류부터_골라_목표_직후에_멈춘다() {
            RemediationPlan plan = planner.planFor(baseline(), 80);

            // 3 → 6(55%) → 9(82%). 82%에서 목표를 넘으므로 남은 두 건은 계획에 넣지 않는다.
            assertThat(plan.documentCount()).isEqualTo(2);
            assertThat(plan.projectedScore()).isEqualTo(82);
            assertThat(plan.achievable()).isTrue();
            assertThat(plan.remainingMissing()).isEqualTo(2);
        }

        @Test
        @DisplayName("동일 가중치는 서류 코드순으로 고정해 결과를 재현 가능하게 한다")
        void 동일_가중치는_서류_코드순으로_고정한다() {
            RemediationPlan plan = planner.planFor(baseline(), 80);

            assertThat(plan.steps())
                    .extracting(step -> step.documentCode().value())
                    .containsExactly("SAFETY_LABEL_SAMPLE", "TEST_REPORT");
        }

        @Test
        @DisplayName("각 단계는 그 단계까지의 준비도와 상승폭을 함께 담는다")
        void 각_단계는_준비도와_상승폭을_담는다() {
            RemediationPlan plan = planner.planFor(baseline(), 80);

            assertThat(plan.steps()).extracting(
                            RemediationStep::order,
                            RemediationStep::scoreAfter,
                            RemediationStep::gainPercentagePoints)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(1, 55, 28),
                            org.assertj.core.groups.Tuple.tuple(2, 82, 27));
        }

        @Test
        @DisplayName("100%를 목표하면 누락 서류를 모두 담는다")
        void 백퍼센트를_목표하면_누락_서류를_모두_담는다() {
            RemediationPlan plan = planner.planFor(baseline(), 100);

            assertThat(plan.documentCount()).isEqualTo(4);
            assertThat(plan.projectedScore()).isEqualTo(100);
            assertThat(plan.remainingMissing()).isZero();
        }
    }

    @Nested
    @DisplayName("경계 상황을 뭉개지 않는다")
    class EdgeCases {

        @Test
        @DisplayName("이미 목표를 넘겼으면 준비할 것이 없다")
        void 이미_목표를_넘겼으면_준비할_것이_없다() {
            RemediationPlan plan = planner.planFor(baseline(), 20);

            assertThat(plan.steps()).isEmpty();
            assertThat(plan.achievable()).isTrue();
            assertThat(plan.projectedScore()).isEqualTo(27);
        }

        @Test
        @DisplayName("점수 산정이 불가능하면(불변식 2) 계획도 산정 불가로 답한다")
        void 점수_산정이_불가능하면_계획도_산정_불가다() {
            ScoreResultView noRequirements =
                    new ScoreResultView(ReadinessScore.notApplicable(), List.of());

            RemediationPlan plan = planner.planFor(noRequirements, 80);

            assertThat(plan.applicable()).isFalse();
            assertThat(plan.steps()).isEmpty();
        }

        @Test
        @DisplayName("목표 준비도가 범위를 벗어나면 도메인이 막는다")
        void 목표_준비도가_범위를_벗어나면_막는다() {
            assertThatThrownBy(() -> planner.planFor(baseline(), 0))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> planner.planFor(baseline(), 101))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
