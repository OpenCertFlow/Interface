package com.certimakers.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 보안 설정. {@code certimakers.security.*}에 바인딩된다.
 *
 * @param encryptionKey 개인정보 암호화 키(Base64, 32바이트 권장). 비어 있으면 개발용 임시 키를
 *                      생성한다 — 운영에서는 반드시 {@code CERTIMAKERS_SECURITY_ENCRYPTION_KEY}로 주입한다.
 */
@ConfigurationProperties(prefix = "certimakers.security")
public record SecurityProperties(String encryptionKey) {
}
