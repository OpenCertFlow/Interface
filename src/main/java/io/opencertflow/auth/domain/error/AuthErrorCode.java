package io.opencertflow.auth.domain.error;

import io.opencertflow.common.domain.error.ErrorCode;
import io.opencertflow.common.domain.error.ErrorType;

/** 인증 컨텍스트 고유 오류. {@code CM-AUTH-<번호>}. */
public enum AuthErrorCode implements ErrorCode {

    /** 이미 가입된 이메일로 회원가입을 시도했다. */
    EMAIL_ALREADY_REGISTERED("OCF-AUTH-001", "이미 가입된 이메일입니다.", ErrorType.CONFLICT),

    /** 이메일 또는 비밀번호가 일치하지 않는다. 어느 쪽이 틀렸는지는 알려주지 않는다. */
    INVALID_CREDENTIALS("OCF-AUTH-002", "이메일 또는 비밀번호가 올바르지 않습니다.", ErrorType.VALIDATION),

    /** 존재하지 않는 사용자다. */
    USER_NOT_FOUND("OCF-AUTH-003", "사용자를 찾을 수 없습니다.", ErrorType.NOT_FOUND),

    /** 이메일 인증 코드가 만료됐거나 일치하지 않는다. */
    EMAIL_VERIFICATION_FAILED("OCF-AUTH-004", "인증 코드가 올바르지 않거나 만료되었습니다.", ErrorType.VALIDATION),

    /** 비밀번호 재설정 토큰이 만료됐거나 일치하지 않는다. */
    PASSWORD_RESET_TOKEN_INVALID("OCF-AUTH-005", "비밀번호 재설정 링크가 올바르지 않거나 만료되었습니다.", ErrorType.VALIDATION),

    /** 소셜 계정에 비밀번호 로그인·재설정을 시도했다. */
    SOCIAL_ACCOUNT_NO_PASSWORD("OCF-AUTH-006", "소셜 로그인으로 가입한 계정입니다. 카카오로 로그인해 주세요.", ErrorType.CONFLICT),

    /** 인증 토큰이 없거나 유효하지 않다. */
    UNAUTHENTICATED("OCF-AUTH-007", "로그인이 필요합니다.", ErrorType.VALIDATION),

    /** 리프레시 토큰이 만료됐거나 일치하지 않는다. */
    INVALID_REFRESH_TOKEN("OCF-AUTH-008", "다시 로그인해 주세요.", ErrorType.VALIDATION),

    /** 카카오 인증 서버 연동에 실패했다. */
    KAKAO_AUTH_FAILED("OCF-AUTH-009", "카카오 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.", ErrorType.EXTERNAL_SERVICE),

    /** 구글 인증 서버 연동에 실패했다. */
    GOOGLE_AUTH_FAILED("OCF-AUTH-010", "구글 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.", ErrorType.EXTERNAL_SERVICE),

    /** 필수 약관에 동의하지 않고 가입을 시도했다. */
    TERMS_NOT_AGREED("OCF-AUTH-011", "필수 약관에 동의해야 가입할 수 있습니다.", ErrorType.VALIDATION),

    /**
     * 짧은 시간에 너무 많이 시도했다.
     *
     * <p>메시지에 남은 횟수나 잠금 해제 시각을 담지 않는다 — 공격자에게 카운터 상태를 알려 주는
     * 것이고, 정상 사용자에게는 "잠시 후"로 충분하다.
     */
    TOO_MANY_ATTEMPTS("OCF-AUTH-012", "시도가 너무 잦습니다. 잠시 후 다시 시도해 주세요.",
            ErrorType.RATE_LIMITED);

    private final String code;
    private final String defaultMessage;
    private final ErrorType type;

    AuthErrorCode(String code, String defaultMessage, ErrorType type) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.type = type;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public ErrorType type() {
        return type;
    }
}
