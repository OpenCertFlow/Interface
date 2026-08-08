package io.opencertflow.auth.application.port.out;

import io.opencertflow.auth.domain.model.PasswordHash;

/**
 * 비밀번호 해싱·대조. 구현은 BCrypt다.
 *
 * <p>BCrypt는 의도적으로 느리게(work factor) 설계되어 CPU를 오래 점유한다. 이벤트 루프에서 직접
 * 호출하면 다른 요청을 막으므로, 호출자(애플리케이션 서비스)는 반드시 BlockingBridge로 감싼다.
 */
public interface PasswordEncoderPort {

    PasswordHash encode(String rawPassword);

    boolean matches(String rawPassword, PasswordHash hash);
}
