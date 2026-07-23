package com.certimakers.auth.application.port.out;

import com.certimakers.auth.domain.model.Email;
import reactor.core.publisher.Mono;

/**
 * 이메일 인증 코드 저장소. 구현은 Redis이며 TTL로 자동 만료된다(예: 5분).
 *
 * <p>인증 코드를 DB가 아니라 Redis에 두는 이유는 (1) 만료가 본질이라 TTL이 곧 도메인 규칙이고,
 * (2) 검증 후 즉시 폐기되는 휘발성 값이라 영속 테이블에 남길 이유가 없기 때문이다.
 */
public interface VerificationCodeStorePort {

    /** 이메일에 인증 코드를 저장한다(TTL 적용). 재요청 시 최신 코드로 덮어쓴다. */
    Mono<Void> save(Email email, String code);

    /** 저장된 코드가 입력값과 일치하는지. 만료됐으면 false. */
    Mono<Boolean> matches(Email email, String code);

    /** 검증 성공 후 코드를 폐기한다. 같은 코드의 재사용을 막는다. */
    Mono<Void> delete(Email email);
}
