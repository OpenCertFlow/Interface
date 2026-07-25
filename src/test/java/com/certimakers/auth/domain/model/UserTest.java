package com.certimakers.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.certimakers.auth.domain.error.AuthErrorCode;
import com.certimakers.common.domain.error.BusinessException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 사용자 애그리거트의 상태 규칙. 스프링 컨텍스트 없이 밀리초 안에 끝난다. */
class UserTest {

    private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");

    private static UserId newId() {
        return UserId.of(com.certimakers.support.TestIds.next());
    }

    private static User localUser() {
        return User.registerLocal(
                newId(), Email.of("user@example.com"), PasswordHash.of("$2a$10$hashed"),
                Nickname.of("테스터"), NOW);
    }

    private static User kakaoUser() {
        return User.registerKakao(
                newId(), Email.of("kakao@example.com"), Nickname.of("카카오"), "123456789", NOW);
    }

    private static User googleUser() {
        return User.registerGoogle(
                newId(), Email.of("google@example.com"), Nickname.of("구글"), "google-sub-abc", NOW);
    }

    @Nested
    @DisplayName("가입 시 초기 상태")
    class Registration {

        @Test
        @DisplayName("로컬 가입은 이메일 미인증 상태로 시작한다")
        void 로컬_가입은_이메일_미인증으로_시작한다() {
            User user = localUser();

            assertThat(user.provider()).isEqualTo(AuthProvider.LOCAL);
            assertThat(user.emailVerified()).isFalse();
            assertThat(user.role()).isEqualTo(Role.USER);
            assertThat(user.passwordHash()).isPresent();
            assertThat(user.providerId()).isEmpty();
        }

        @Test
        @DisplayName("카카오 가입은 이미 검증된 이메일이므로 인증 완료로 시작한다")
        void 카카오_가입은_인증완료로_시작한다() {
            User user = kakaoUser();

            assertThat(user.provider()).isEqualTo(AuthProvider.KAKAO);
            assertThat(user.emailVerified()).isTrue();
            assertThat(user.passwordHash()).isEmpty();
            assertThat(user.providerId()).contains("123456789");
        }

        @Test
        @DisplayName("구글 가입도 이미 검증된 이메일이므로 인증 완료·비밀번호 없음으로 시작한다")
        void 구글_가입은_인증완료로_시작한다() {
            User user = googleUser();

            assertThat(user.provider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(user.emailVerified()).isTrue();
            assertThat(user.passwordHash()).isEmpty();
            assertThat(user.providerId()).contains("google-sub-abc");
        }
    }

    @Nested
    @DisplayName("소셜 계정은 비밀번호를 가질 수 없다")
    class SocialAccountHasNoPassword {

        @Test
        @DisplayName("카카오 계정의 비밀번호 검증을 시도하면 거부한다")
        void 카카오_계정의_비밀번호_검증을_거부한다() {
            User user = kakaoUser();

            assertThatThrownBy(user::passwordHashForVerification)
                    .isInstanceOf(BusinessException.class)
                    .satisfies(error -> assertThat(((BusinessException) error).errorCode())
                            .isEqualTo(AuthErrorCode.SOCIAL_ACCOUNT_NO_PASSWORD));
        }

        @Test
        @DisplayName("카카오 계정의 비밀번호 변경을 시도하면 거부한다 — 로그인 경로가 두 갈래가 되면 안 된다")
        void 카카오_계정의_비밀번호_변경을_거부한다() {
            User user = kakaoUser();

            assertThatThrownBy(() -> user.changePassword(PasswordHash.of("$2a$10$new")))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("로컬 계정은 비밀번호를 바꿀 수 있다")
        void 로컬_계정은_비밀번호를_바꿀_수_있다() {
            User user = localUser();

            user.changePassword(PasswordHash.of("$2a$10$changed"));

            assertThat(user.passwordHashForVerification().value()).isEqualTo("$2a$10$changed");
        }
    }

    @Nested
    @DisplayName("상태 변경")
    class StateChanges {

        @Test
        @DisplayName("이메일 인증은 멱등이다 — 두 번 호출해도 문제없다")
        void 이메일_인증은_멱등이다() {
            User user = localUser();

            user.verifyEmail();
            user.verifyEmail();

            assertThat(user.emailVerified()).isTrue();
        }

        @Test
        @DisplayName("닉네임을 바꿀 수 있다")
        void 닉네임을_바꿀_수_있다() {
            User user = localUser();

            user.changeNickname(Nickname.of("새이름"));

            assertThat(user.nickname().value()).isEqualTo("새이름");
        }
    }

    @Test
    @DisplayName("동일성은 식별자로만 판단한다 — 값이 같아도 id가 다르면 다른 사용자다")
    void 동일성은_식별자로만_판단한다() {
        UserId sharedId = newId();
        User first = User.registerLocal(
                sharedId, Email.of("a@example.com"), PasswordHash.of("$2a$10$x"),
                Nickname.of("이름A"), NOW);
        User second = User.registerLocal(
                sharedId, Email.of("b@example.com"), PasswordHash.of("$2a$10$y"),
                Nickname.of("이름B"), NOW);

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(localUser());
    }
}
