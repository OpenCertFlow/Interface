package com.certimakers.auth.application.port.in;

import reactor.core.publisher.Mono;

/** 로그아웃. 저장된 리프레시 토큰을 폐기해 이후 재발급을 막는다. */
public interface LogoutUseCase {

    Mono<Void> logout(String userId);
}
