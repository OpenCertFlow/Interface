package com.certimakers.auth.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 인증 API 요청 DTO 모음. 한 파일에 모아 두는 이유는 각각이 작고 함께 읽히기 때문이다.
 *
 * <p>비밀번호 최소 길이 같은 <b>형식</b> 검증은 여기서 하고, "이미 가입된 이메일인가" 같은
 * <b>규칙</b> 검증은 도메인·서비스가 한다.
 */
public final class AuthRequests {

    private AuthRequests() {
    }

    public record SignUp(
            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해 주세요.")
            @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해 주세요.")
            String password,

            @NotBlank(message = "닉네임을 입력해 주세요.")
            String nickname,

            /** 동의한 약관 키. 필수 약관이 모두 포함되어야 가입된다(F-AUTH-008). */
            java.util.List<String> agreedTermKeys) {
    }

    public record Login(
            @NotBlank(message = "이메일을 입력해 주세요.")
            String email,

            @NotBlank(message = "비밀번호를 입력해 주세요.")
            String password) {
    }

    public record KakaoLogin(
            @NotBlank(message = "인가 코드가 필요합니다.")
            String code) {
    }

    public record GoogleLogin(
            @NotBlank(message = "인가 코드가 필요합니다.")
            String code) {
    }

    public record Refresh(
            @NotBlank(message = "리프레시 토큰이 필요합니다.")
            String refreshToken) {
    }

    /** 로그아웃(F-AUTH-013). 폐기할 현재 세션을 리프레시 토큰으로 식별한다. */
    public record Logout(
            @NotBlank(message = "리프레시 토큰이 필요합니다.")
            String refreshToken) {
    }

    public record SendEmailCode(
            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email) {
    }

    public record VerifyEmailCode(
            @NotBlank(message = "이메일을 입력해 주세요.")
            String email,

            @NotBlank(message = "인증 코드를 입력해 주세요.")
            String code) {
    }

    public record RequestPasswordReset(
            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email) {
    }

    public record ResetPassword(
            @NotBlank(message = "재설정 토큰이 필요합니다.")
            String token,

            @NotBlank(message = "새 비밀번호를 입력해 주세요.")
            @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해 주세요.")
            String newPassword) {
    }

    public record UpdateNickname(
            @NotBlank(message = "닉네임을 입력해 주세요.")
            String nickname) {
    }

    public record ChangePassword(
            @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
            String currentPassword,

            @NotBlank(message = "새 비밀번호를 입력해 주세요.")
            @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해 주세요.")
            String newPassword) {
    }
}
