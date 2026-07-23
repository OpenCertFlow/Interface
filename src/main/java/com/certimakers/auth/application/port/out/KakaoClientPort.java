package com.certimakers.auth.application.port.out;

import reactor.core.publisher.Mono;

/**
 * 카카오 OAuth 연동. 인가 코드로 카카오에서 사용자 프로필을 가져온다.
 *
 * <p>WebClient 기반이라 논블로킹이다. 두 단계(코드 → 액세스 토큰 → 프로필)를 어댑터가 감춘다 —
 * 애플리케이션은 "인가 코드를 주면 프로필이 온다"만 안다.
 */
public interface KakaoClientPort {

    Mono<KakaoProfile> fetchProfile(String authorizationCode);

    /**
     * 카카오가 준 최소 프로필. 이메일 제공에 동의하지 않은 사용자는 email이 null일 수 있으므로,
     * 호출자가 그 경우를 처리한다.
     *
     * @param kakaoId  카카오 회원번호(고유). 계정 매칭의 기준
     * @param email    카카오 계정 이메일. 미동의 시 null
     * @param nickname 카카오 프로필 닉네임
     */
    record KakaoProfile(String kakaoId, String email, String nickname) {
    }
}
