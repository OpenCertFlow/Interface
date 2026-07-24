package com.certimakers.auth.application.port.out;

import reactor.core.publisher.Mono;

/**
 * 구글 OAuth 연동. 인가 코드로 구글에서 사용자 프로필을 가져온다.
 *
 * <p>{@link KakaoClientPort}과 같은 구조다 — WebClient 기반 논블로킹이며, 두 단계(코드 → 액세스
 * 토큰 → 프로필)를 어댑터가 감춘다. 애플리케이션은 "인가 코드를 주면 프로필이 온다"만 안다.
 */
public interface GoogleClientPort {

    Mono<GoogleProfile> fetchProfile(String authorizationCode);

    /**
     * 구글이 준 최소 프로필. 구글 OpenID Connect는 이메일을 항상 제공하지만, 사용자가 이메일 범위에
     * 동의하지 않았을 가능성에 대비해 호출자가 null을 처리한다.
     *
     * @param googleId 구글 계정 고유 식별자(OIDC {@code sub}). 계정 매칭의 기준
     * @param email    구글 계정 이메일. 미제공 시 null
     * @param name     구글 프로필 이름
     */
    record GoogleProfile(String googleId, String email, String name) {
    }
}
