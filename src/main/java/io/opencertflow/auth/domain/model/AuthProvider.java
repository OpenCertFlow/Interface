package io.opencertflow.auth.domain.model;

/**
 * 계정의 인증 출처.
 *
 * <p>{@link #LOCAL}은 이메일·비밀번호로 직접 가입한 계정, {@link #KAKAO}·{@link #GOOGLE}은 각 소셜
 * OAuth로 만들어진 계정이다. 소셜 계정은 비밀번호가 없으므로 비밀번호 로그인·재설정을 시도하면
 * 도메인이 거부한다.
 */
public enum AuthProvider {

    LOCAL("이메일"),
    KAKAO("카카오"),
    GOOGLE("구글");

    private final String displayName;

    AuthProvider(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isSocial() {
        return this != LOCAL;
    }
}
