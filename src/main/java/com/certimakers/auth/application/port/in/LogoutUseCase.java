package com.certimakers.auth.application.port.in;

import reactor.core.publisher.Mono;

/**
 * 로그아웃(F-AUTH-013). 현재 세션의 리프레시 토큰만 폐기하고, 같은 계정의 다른 기기 세션은 유지한다.
 * 어떤 세션인지는 클라이언트가 보낸 리프레시 토큰으로 식별한다.
 */
public interface LogoutUseCase {

    Mono<Void> logout(String userId, String refreshToken);
}
