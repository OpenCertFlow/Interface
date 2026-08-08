package io.opencertflow.auth.application.port.in;

import io.opencertflow.auth.application.port.out.TokenProviderPort.IssuedTokens;
import reactor.core.publisher.Mono;

/**
 * 구글 로그인. 인가 코드로 프로필을 받아, 기존 계정이면 로그인하고 없으면 즉시 가입시킨다
 * (신규 여부와 무관하게 토큰을 발급하는 소셜 로그인 관례).
 */
public interface GoogleLoginUseCase {

    Mono<IssuedTokens> login(GoogleLoginCommand command);

    record GoogleLoginCommand(String authorizationCode) {
    }
}
