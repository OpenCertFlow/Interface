package com.certimakers.document.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * 발급자 표시 이름 조회. 인증 컨텍스트를 향한 포트다.
 *
 * <p>PDF에 "발급자: 홍길동"을 찍기 위해서만 쓴다. 인증 컨텍스트의 애그리거트를 물지 않고 필요한
 * 값 하나만 가져온다.
 */
public interface LoadIssuerNamePort {

    Optional<String> findNickname(UUID issuerId);
}
