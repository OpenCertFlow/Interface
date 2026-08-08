package io.opencertflow.common.domain.model;

import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.common.domain.error.CommonErrorCode;
import java.util.Collection;

/**
 * 도메인 불변식 검증. 스프링의 {@code Assert}를 쓰지 않는 이유는 도메인이 프레임워크를 참조하면
 * 안 되기 때문이다(ArchUnit이 이를 강제한다).
 *
 * <p>불변식 위반은 {@link BusinessException}(VALIDATION)으로 던진다. 값 객체 생성자에서 호출한다.
 */
public final class Guard {

    private Guard() {
    }

    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw BusinessException.invalid("%s 값이 필요합니다.".formatted(name));
        }
        return value;
    }

    public static String hasText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid("%s 값이 비어 있습니다.".formatted(name));
        }
        return value;
    }

    public static <C extends Collection<?>> C notEmpty(C value, String name) {
        if (value == null || value.isEmpty()) {
            throw BusinessException.invalid("%s 목록이 비어 있습니다.".formatted(name));
        }
        return value;
    }

    public static int inRange(int value, int minInclusive, int maxInclusive, String name) {
        if (value < minInclusive || value > maxInclusive) {
            throw BusinessException.invalid(
                    "%s 값은 %d 이상 %d 이하여야 합니다. (입력: %d)".formatted(name, minInclusive, maxInclusive, value));
        }
        return value;
    }

    public static int positive(int value, String name) {
        if (value <= 0) {
            throw BusinessException.invalid("%s 값은 0보다 커야 합니다. (입력: %d)".formatted(name, value));
        }
        return value;
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw BusinessException.invalid(message);
        }
    }

    public static void state(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(CommonErrorCode.ILLEGAL_STATE, message);
        }
    }
}
