package io.opencertflow.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opencertflow.common.domain.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 이메일 값 객체. 정규화가 핵심이다 — 대소문자만 다른 주소가 서로 다른 계정으로 취급되면
 * 같은 사람이 두 번 가입하거나 로그인에 실패한다.
 */
class EmailTest {

    @Test
    @DisplayName("대문자와 공백을 정규화해 같은 주소를 같은 값으로 만든다")
    void 대문자와_공백을_정규화한다() {
        Email upper = Email.of("  User@Example.COM  ");
        Email lower = Email.of("user@example.com");

        assertThat(upper.value()).isEqualTo("user@example.com");
        assertThat(upper).isEqualTo(lower);
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "@no-local.com", "no-at-sign.com", "no@domain", "a@b@c.com"})
    @DisplayName("명백히 잘못된 형식은 거부한다")
    void 잘못된_형식은_거부한다(String invalid) {
        assertThatThrownBy(() -> Email.of(invalid))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빈 값은 거부한다")
    void 빈_값은_거부한다() {
        assertThatThrownBy(() -> Email.of("  "))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(BusinessException.class);
    }
}
