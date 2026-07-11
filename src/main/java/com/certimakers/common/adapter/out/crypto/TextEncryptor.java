package com.certimakers.common.adapter.out.crypto;

/**
 * 필드 단위 문자열 암호화. 연락처 같은 개인정보를 저장 전 암호화하고 읽을 때 복호화한다.
 *
 * <p>인프라 관심사이므로 어댑터 계층에 둔다. 도메인은 평문 {@code ContactInfo}만 다루고, 암호화는
 * 영속성 매퍼가 이 인터페이스로 수행한다 — 도메인은 암호화를 모른다.
 */
public interface TextEncryptor {

    /** 평문을 암호화해 저장 가능한 문자열(Base64)로 만든다. */
    String encrypt(String plaintext);

    /** {@link #encrypt}가 만든 문자열을 평문으로 되돌린다. */
    String decrypt(String ciphertext);
}
