package com.certimakers.auth.application.port.out;

import reactor.core.publisher.Mono;

/**
 * 리프레시 토큰 저장소. 구현은 Redis다(TTL로 자동 만료).
 *
 * <p>리프레시 토큰을 서버에 두는 이유는 <b>로그아웃·강제 만료</b> 때문이다. 순수 JWT만 쓰면 발급된
 * 토큰을 서버가 취소할 수 없다. 저장된 값과 대조하므로, 삭제하면 그 즉시 무효가 된다.
 *
 * <p>Lettuce 기반이라 논블로킹이다. BlockingBridge로 감쌀 필요가 없다.
 */
public interface RefreshTokenStorePort {

    /** userId에 리프레시 토큰을 저장한다(TTL 적용). 기존 값이 있으면 덮어써 단일 세션을 유지한다. */
    Mono<Void> save(String userId, String refreshToken);

    /** 저장된 리프레시 토큰이 주어진 값과 일치하는지. 로그아웃·재발급된 토큰이면 false. */
    Mono<Boolean> matches(String userId, String refreshToken);

    /** 로그아웃. 저장된 토큰을 지워 이후 재발급을 막는다. */
    Mono<Void> delete(String userId);
}
