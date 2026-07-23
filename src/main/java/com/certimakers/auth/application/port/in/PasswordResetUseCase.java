package com.certimakers.auth.application.port.in;

import reactor.core.publisher.Mono;

/** 비밀번호 찾기: 재설정 링크 요청과 새 비밀번호 설정. */
public interface PasswordResetUseCase {

    /**
     * 재설정 토큰을 만들어 Redis에 저장하고 링크를 이메일로 보낸다.
     *
     * <p><b>계정 존재 여부를 응답으로 드러내지 않는다.</b> 가입되지 않은 이메일이어도 성공으로
     * 응답한다 — 그래야 공격자가 이 API로 가입 여부를 캐낼 수 없다(계정 열거 방지).
     */
    Mono<Void> requestReset(String email);

    /** 토큰을 검증하고 새 비밀번호로 교체한다. */
    Mono<Void> reset(ResetPasswordCommand command);

    record ResetPasswordCommand(String token, String newRawPassword) {
    }
}
