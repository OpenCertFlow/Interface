package com.certimakers.auth.adapter.out.security;

import com.certimakers.auth.application.port.out.PasswordEncoderPort;
import com.certimakers.auth.domain.model.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * {@link PasswordEncoderPort}의 BCrypt 구현.
 *
 * <p>BCrypt는 의도적으로 느리다 — 무차별 대입을 어렵게 하는 것이 목적이다. 그래서 이 어댑터의
 * 메서드는 CPU를 오래 점유하며, 호출자(애플리케이션 서비스)는 반드시 BlockingBridge로 감싼다.
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public PasswordHash encode(String rawPassword) {
        return PasswordHash.of(delegate.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        return delegate.matches(rawPassword, hash.value());
    }
}
