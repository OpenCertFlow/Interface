package com.certimakers.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.certimakers.common.domain.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 닉네임 값 객체. 화면에 그대로 노출되므로 길이와 공백을 강제한다. */
class NicknameTest {

    @Test
    @DisplayName("앞뒤 공백을 제거한다")
    void 앞뒤_공백을_제거한다() {
        assertThat(Nickname.of("  테스터  ").value()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("너무 짧거나 긴 이름은 거부한다")
    void 길이를_강제한다() {
        assertThatThrownBy(() -> Nickname.of("가"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Nickname.of("가".repeat(21)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빈 값은 거부한다")
    void 빈_값은_거부한다() {
        assertThatThrownBy(() -> Nickname.of("   "))
                .isInstanceOf(BusinessException.class);
    }
}
