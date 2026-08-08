package io.opencertflow.auth.application.port.in;

import reactor.core.publisher.Mono;

/** 이메일 인증 코드 발송과 확인. */
public interface EmailVerificationUseCase {

    /** 인증 코드를 생성해 Redis에 저장하고 이메일로 보낸다. */
    Mono<Void> sendCode(String email);

    /** 코드를 대조해 성공하면 해당 사용자의 이메일 인증을 완료한다. */
    Mono<Void> verify(VerifyEmailCommand command);

    record VerifyEmailCommand(String email, String code) {
    }
}
