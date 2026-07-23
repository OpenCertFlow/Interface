package com.certimakers.auth.application.port.out;

import com.certimakers.auth.domain.model.Email;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * 비밀번호 재설정 토큰 저장소. 구현은 Redis이며 TTL로 자동 만료된다(예: 30분).
 *
 * <p>토큰 → 이메일 방향으로 저장한다. 재설정 링크에는 토큰만 담기고, 서버가 그 토큰으로 대상
 * 이메일을 되찾는다. 이메일을 링크에 노출하지 않아 계정 열거 공격 표면을 줄인다.
 */
public interface PasswordResetTokenStorePort {

    /** 재설정 토큰과 대상 이메일을 저장한다(TTL 적용). */
    Mono<Void> save(String token, Email email);

    /** 토큰에 연결된 이메일을 찾는다. 만료·부재면 비어 있음. */
    Mono<Optional<Email>> findEmail(String token);

    /** 재설정 완료 후 토큰을 폐기한다. 링크 재사용을 막는다. */
    Mono<Void> delete(String token);
}
