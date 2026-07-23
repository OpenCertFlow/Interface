package com.certimakers.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인증 설정. {@code certimakers.auth.*}에 바인딩된다.
 *
 * @param jwt                  JWT 서명·만료 설정
 * @param kakao                카카오 OAuth 설정
 * @param email                이메일 인증·재설정 설정
 * @param bootstrapAdminEmails 시작 시 관리자로 승격할 계정 이메일. 최초 관리자를 만드는 유일한 통로다
 */
@ConfigurationProperties(prefix = "certimakers.auth")
public record AuthProperties(
        Jwt jwt, Kakao kakao, EmailVerification email, List<String> bootstrapAdminEmails) {

    /**
     * @param secret                  HMAC 서명 비밀키(Base64 또는 평문). 최소 32바이트여야 HS256에 안전하다
     * @param accessTokenTtlSeconds   액세스 토큰 수명(초). 짧게 둔다
     * @param refreshTokenTtlSeconds  리프레시 토큰 수명(초). 길게 둔다
     * @param issuer                  토큰 발급자 식별자
     */
    public record Jwt(
            String secret,
            long accessTokenTtlSeconds,
            long refreshTokenTtlSeconds,
            String issuer) {
    }

    /**
     * @param clientId     카카오 REST API 키
     * @param clientSecret 카카오 클라이언트 시크릿(선택)
     * @param redirectUri  인가 코드를 받을 리다이렉트 URI
     * @param tokenUri     액세스 토큰 발급 엔드포인트
     * @param userInfoUri  사용자 정보 조회 엔드포인트
     */
    public record Kakao(
            String clientId,
            String clientSecret,
            String redirectUri,
            String tokenUri,
            String userInfoUri) {
    }

    /**
     * @param codeTtlSeconds       인증 코드 수명(초)
     * @param resetTokenTtlSeconds 재설정 토큰 수명(초)
     * @param resetLinkBaseUrl     재설정 링크의 기반 URL. 여기에 토큰을 붙여 보낸다
     * @param from                 발신 이메일 주소
     */
    public record EmailVerification(
            long codeTtlSeconds,
            long resetTokenTtlSeconds,
            String resetLinkBaseUrl,
            String from) {
    }
}
