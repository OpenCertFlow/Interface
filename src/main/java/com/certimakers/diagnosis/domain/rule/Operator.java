package com.certimakers.diagnosis.domain.rule;

import com.certimakers.common.domain.error.BusinessException;
import java.util.Collection;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * 속성 값과 기대 값을 비교하는 연산자.
 *
 * <p>널 처리 규약이 중요하다. 값이 없는 속성(전기 미사용 제품의 전압)에 대해 순서 비교({@code GT},
 * {@code GTE}, {@code LT}, {@code LTE})는 <b>항상 false</b>를 반환한다. 즉 "전압 &gt; 50"은 전압이
 * 없을 때 매칭되지 않는다. 이는 조용한 오판이 아니라 의도된 것이다 — 값 부재로 인한 판단 불가는
 * 룰 작성자가 별도의 AMBIGUOUS_CONDITION 룰로 명시적으로 표현한다.
 */
public enum Operator {

    EQ {
        @Override
        public boolean test(Object actual, Object expected) {
            return Objects.equals(actual, expected);
        }
    },
    NEQ {
        @Override
        public boolean test(Object actual, Object expected) {
            return !Objects.equals(actual, expected);
        }
    },
    GT {
        @Override
        public boolean test(Object actual, Object expected) {
            return numericCompare(actual, expected).stream().anyMatch(c -> c > 0);
        }
    },
    GTE {
        @Override
        public boolean test(Object actual, Object expected) {
            return numericCompare(actual, expected).stream().anyMatch(c -> c >= 0);
        }
    },
    LT {
        @Override
        public boolean test(Object actual, Object expected) {
            return numericCompare(actual, expected).stream().anyMatch(c -> c < 0);
        }
    },
    LTE {
        @Override
        public boolean test(Object actual, Object expected) {
            return numericCompare(actual, expected).stream().anyMatch(c -> c <= 0);
        }
    },
    /** actual 값이 기대 집합(expected: Collection)에 포함되는가. */
    IN {
        @Override
        public boolean test(Object actual, Object expected) {
            if (!(expected instanceof Collection<?> options)) {
                throw BusinessException.invalid("IN 연산자의 기대 값은 컬렉션이어야 합니다.");
            }
            return actual != null && options.contains(actual);
        }
    },
    /** actual 집합(actual: Collection)이 기대 값을 포함하는가. */
    CONTAINS {
        @Override
        public boolean test(Object actual, Object expected) {
            if (actual == null) {
                return false;
            }
            if (!(actual instanceof Collection<?> elements)) {
                throw BusinessException.invalid("CONTAINS 연산자의 대상 속성은 컬렉션이어야 합니다.");
            }
            return elements.contains(expected);
        }
    };

    public abstract boolean test(Object actual, Object expected);

    /**
     * 두 값의 순서 비교. 한쪽이라도 null이면 비어 있는 {@link OptionalInt}를 반환하여, 호출한
     * 순서 연산자가 전부 false로 귀결되게 한다. 숫자가 아닌 값에 순서 비교를 시도하면 예외다 —
     * 룰 정의 오류이므로 조용히 넘기지 않는다.
     */
    private static OptionalInt numericCompare(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return OptionalInt.empty();
        }
        if (actual instanceof Number a && expected instanceof Number b) {
            return OptionalInt.of(Double.compare(a.doubleValue(), b.doubleValue()));
        }
        throw BusinessException.invalid(
                "순서 비교는 숫자 속성에만 적용됩니다. (actual=%s, expected=%s)"
                        .formatted(typeName(actual), typeName(expected)));
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
