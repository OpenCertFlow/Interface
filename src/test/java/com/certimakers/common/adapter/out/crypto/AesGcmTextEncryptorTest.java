package com.certimakers.common.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.certimakers.common.domain.error.BusinessException;
import java.security.SecureRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AesGcmTextEncryptorTest {

    private final AesGcmTextEncryptor encryptor = new AesGcmTextEncryptor(key());

    private static byte[] key() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    @Test
    @DisplayName("암호화한 값을 복호화하면 원문으로 돌아온다")
    void 왕복() {
        String plaintext = "010-1234-5678";

        String encrypted = encryptor.encrypt(plaintext);
        String decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(encrypted).isNotEqualTo(plaintext); // 실제로 암호화됐다
    }

    @Test
    @DisplayName("같은 평문도 매번 다른 암호문이 된다 — IV가 매번 새로 생성되므로")
    void 비결정적_암호문() {
        String plaintext = "hong@example.com";

        String first = encryptor.encrypt(plaintext);
        String second = encryptor.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
        // 그래도 둘 다 같은 원문으로 복호화된다
        assertThat(encryptor.decrypt(first)).isEqualTo(plaintext);
        assertThat(encryptor.decrypt(second)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("암호문이 변조되면 GCM 인증 태그가 복호화를 거부한다")
    void 변조_탐지() {
        String encrypted = encryptor.encrypt("민감정보");
        // 마지막 문자를 바꿔 변조를 흉내 낸다
        char last = encrypted.charAt(encrypted.length() - 1);
        String tampered = encrypted.substring(0, encrypted.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThatThrownBy(() -> encryptor.decrypt(tampered))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("null은 그대로 null로 통과한다 — 선택 필드(이메일) 처리")
    void null_통과() {
        assertThat(encryptor.encrypt(null)).isNull();
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("한글·이모지 등 UTF-8 전 범위를 보존한다")
    void 유니코드_보존() {
        String plaintext = "홍길동 서울시 강남구 😀";
        assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
    }
}
