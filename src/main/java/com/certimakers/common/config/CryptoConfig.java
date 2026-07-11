package com.certimakers.common.config;

import com.certimakers.common.adapter.out.crypto.AesGcmTextEncryptor;
import com.certimakers.common.adapter.out.crypto.TextEncryptor;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 개인정보 암호화기를 조립한다.
 *
 * <p>키를 하드코딩하지 않는다. 설정이 있으면 그것을 쓰고, 없으면 시작 시 임시 키를 생성하며 경고한다.
 * 임시 키는 재시작 시 바뀌므로 이전 데이터를 복호화할 수 없다 — 개발·데모 전용이다. 운영에서는
 * {@code CERTIMAKERS_SECURITY_ENCRYPTION_KEY} 환경변수로 고정 키를 주입해야 한다.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class CryptoConfig {

    private static final Logger log = LoggerFactory.getLogger(CryptoConfig.class);
    private static final int KEY_BYTES = 32; // AES-256

    @Bean
    public TextEncryptor textEncryptor(SecurityProperties properties) {
        byte[] key = resolveKey(properties.encryptionKey());
        return new AesGcmTextEncryptor(key);
    }

    private byte[] resolveKey(String configured) {
        if (StringUtils.hasText(configured)) {
            byte[] key = Base64.getDecoder().decode(configured.trim());
            log.info("개인정보 암호화: 설정된 키를 사용합니다. ({}비트)", key.length * 8);
            return key;
        }
        byte[] ephemeral = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(ephemeral);
        log.warn("개인정보 암호화: 설정된 키가 없어 임시 키를 생성했습니다. 재시작 시 이전 데이터를 "
                + "복호화할 수 없습니다. 운영에서는 certimakers.security.encryption-key를 반드시 설정하세요.");
        return ephemeral;
    }
}
