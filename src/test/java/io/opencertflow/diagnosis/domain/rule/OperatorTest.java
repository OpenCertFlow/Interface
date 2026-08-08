package io.opencertflow.diagnosis.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opencertflow.common.domain.error.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OperatorTest {

    @Nested
    @DisplayName("순서 비교에서 null actual은 항상 false — '전압>50'은 전압 미상일 때 매칭되지 않는다")
    class NullOrdering {

        @Test
        void GT_LT_GTE_LTE_모두_null에서_false() {
            // 이것이 초기 구현의 버그 지점이었다. LT가 null에서 true가 되면 안 된다.
            assertThat(Operator.GT.test(null, 50)).isFalse();
            assertThat(Operator.GTE.test(null, 50)).isFalse();
            assertThat(Operator.LT.test(null, 50)).isFalse();
            assertThat(Operator.LTE.test(null, 50)).isFalse();
        }

        @Test
        void 기대값이_null이어도_순서비교는_false() {
            assertThat(Operator.GT.test(100, null)).isFalse();
            assertThat(Operator.LT.test(100, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("순서 비교 정상 동작")
    class Ordering {

        @Test
        void GT() {
            assertThat(Operator.GT.test(220, 50)).isTrue();
            assertThat(Operator.GT.test(50, 50)).isFalse();
            assertThat(Operator.GT.test(30, 50)).isFalse();
        }

        @Test
        void GTE_경계값_포함() {
            assertThat(Operator.GTE.test(50, 50)).isTrue();
        }

        @Test
        void 숫자가_아닌_값에_순서비교하면_룰_정의_오류로_예외() {
            assertThatThrownBy(() -> Operator.GT.test("high", "low"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("동등·집합 비교")
    class EqualityAndSets {

        @Test
        void EQ_NEQ() {
            assertThat(Operator.EQ.test(true, true)).isTrue();
            assertThat(Operator.EQ.test(null, null)).isTrue();
            assertThat(Operator.NEQ.test(220, 110)).isTrue();
        }

        @Test
        void IN_기대집합에_포함되는가() {
            assertThat(Operator.IN.test("A", List.of("A", "B"))).isTrue();
            assertThat(Operator.IN.test("C", List.of("A", "B"))).isFalse();
            assertThat(Operator.IN.test(null, List.of("A"))).isFalse();
        }

        @Test
        void IN_기대값이_컬렉션이_아니면_예외() {
            assertThatThrownBy(() -> Operator.IN.test("A", "not-a-collection"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void CONTAINS_대상집합이_원소를_포함하는가() {
            assertThat(Operator.CONTAINS.test(List.of("A", "B"), "A")).isTrue();
            assertThat(Operator.CONTAINS.test(List.of("A", "B"), "C")).isFalse();
            assertThat(Operator.CONTAINS.test(null, "A")).isFalse();
        }
    }
}
