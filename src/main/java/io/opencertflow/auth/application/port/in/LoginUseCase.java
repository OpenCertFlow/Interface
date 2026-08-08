package io.opencertflow.auth.application.port.in;

import io.opencertflow.auth.application.port.out.TokenProviderPort.IssuedTokens;
import reactor.core.publisher.Mono;

/** 이메일·비밀번호 로그인. 성공 시 액세스·리프레시 토큰을 발급한다. */
public interface LoginUseCase {

    Mono<IssuedTokens> login(LoginCommand command);

    record LoginCommand(String email, String rawPassword) {
    }
}
