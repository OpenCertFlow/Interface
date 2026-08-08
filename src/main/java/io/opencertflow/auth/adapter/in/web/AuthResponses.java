package io.opencertflow.auth.adapter.in.web;

import io.opencertflow.auth.application.port.out.TokenProviderPort.IssuedTokens;
import io.opencertflow.auth.domain.model.User;

/** 인증 API 응답 DTO 모음. */
public final class AuthResponses {

    private AuthResponses() {
    }

    /**
     * 로그인·재발급 응답.
     *
     * @param accessToken           Authorization 헤더에 Bearer로 실어 보낼 토큰
     * @param refreshToken          액세스 토큰 만료 시 재발급에 쓸 토큰
     * @param expiresInSeconds      액세스 토큰 남은 수명(초)
     * @param tokenType             항상 "Bearer"
     */
    public record Tokens(
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            String tokenType) {

        public static Tokens from(IssuedTokens issued) {
            return new Tokens(
                    issued.accessToken(),
                    issued.refreshToken(),
                    issued.accessTokenExpiresInSeconds(),
                    "Bearer");
        }
    }

    /** 회원가입 응답. 가입 직후에는 토큰을 주지 않고 로그인을 유도한다. */
    public record SignedUp(String userId) {
    }

    /**
     * 마이페이지 응답. <b>비밀번호 해시는 절대 포함하지 않는다.</b>
     *
     * @param provider      가입 경로(LOCAL·KAKAO). 클라이언트가 비밀번호 변경 UI 노출을 판단한다
     * @param emailVerified 이메일 인증 완료 여부
     */
    public record Profile(
            String userId,
            String email,
            String nickname,
            String role,
            String provider,
            boolean emailVerified,
            String createdAt) {

        public static Profile from(User user) {
            return new Profile(
                    user.id().value().toString(),
                    user.email().value(),
                    user.nickname().value(),
                    user.role().name(),
                    user.provider().name(),
                    user.emailVerified(),
                    user.createdAt().toString());
        }
    }
}
