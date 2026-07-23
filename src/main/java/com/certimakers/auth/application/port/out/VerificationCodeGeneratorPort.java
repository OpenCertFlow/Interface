package com.certimakers.auth.application.port.out;

/**
 * 인증 코드·재설정 토큰 생성. 보안 난수를 쓰므로 도메인이 아니라 어댑터에 둔다.
 *
 * <p>도메인은 {@code Instant.now()}·{@code SecureRandom}을 직접 만질 수 없다(ArchUnit). 난수 생성을
 * 포트로 밀어내면 서비스 테스트에서 코드를 고정할 수 있어 흐름 검증이 결정적으로 된다.
 */
public interface VerificationCodeGeneratorPort {

    /** 6자리 숫자 인증 코드. 이메일로 보내 사용자가 입력한다. */
    String newNumericCode();

    /** 추측 불가능한 재설정 토큰. 링크에 담긴다. */
    String newResetToken();
}
