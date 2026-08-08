package io.opencertflow.auth.application.port.in;

import io.opencertflow.auth.application.port.out.TokenProviderPort.IssuedTokens;
import reactor.core.publisher.Mono;

/** 리프레시 토큰으로 새 액세스·리프레시 토큰 쌍을 발급한다(회전). */
public interface RefreshTokenUseCase {

    Mono<IssuedTokens> refresh(RefreshCommand command);

    record RefreshCommand(String refreshToken) {
    }
}
