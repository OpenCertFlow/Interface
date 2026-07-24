package com.certimakers.auth.application.port.in;

import reactor.core.publisher.Mono;

/** 계정 탈퇴(F-AUTH-018). 사용자 계정을 삭제하고 세션(리프레시 토큰)을 폐기한다. */
public interface WithdrawAccountUseCase {

    Mono<Void> withdraw(String userId);
}
