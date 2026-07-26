package com.certimakers.auth.application.port.out;

import reactor.core.publisher.Mono;

/**
 * 리프레시 토큰 저장소. 구현은 Redis다(TTL로 자동 만료).
 *
 * <p>리프레시 토큰을 서버에 두는 이유는 <b>로그아웃·강제 만료</b> 때문이다. 순수 JWT만 쓰면 발급된
 * 토큰을 서버가 취소할 수 없다. 저장된 값과 대조하므로, 삭제하면 그 즉시 무효가 된다.
 *
 * <p><b>다중 세션(F-AUTH-013).</b> 한 사용자가 웹·앱·여러 기기에서 동시에 로그인할 수 있어야 하므로,
 * 토큰을 사용자당 하나로 덮어쓰지 않고 <b>세션(토큰)별로</b> 저장한다. 로그아웃은 그 세션만 지우고
 * 다른 세션은 유지한다({@link #delete}). 탈퇴·강제 로그아웃은 전부 지운다({@link #deleteAll}).
 *
 * <p>Lettuce 기반이라 논블로킹이다. BlockingBridge로 감쌀 필요가 없다.
 */
public interface RefreshTokenStorePort {

    /** userId에 리프레시 토큰(세션)을 추가한다(TTL 적용). 같은 사용자의 다른 세션은 건드리지 않는다. */
    Mono<Void> save(String userId, String refreshToken);

    /** 주어진 리프레시 토큰이 이 사용자의 유효한 세션인지. 로그아웃·재발급(회전)된 토큰이면 false. */
    Mono<Boolean> matches(String userId, String refreshToken);

    /** 로그아웃. 이 리프레시 토큰의 세션 하나만 지운다 — 다른 기기 세션은 유지된다. */
    Mono<Void> delete(String userId, String refreshToken);

    /** 계정 탈퇴·강제 로그아웃. 이 사용자의 모든 세션을 지운다. */
    Mono<Void> deleteAll(String userId);
}
