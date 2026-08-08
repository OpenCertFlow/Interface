package io.opencertflow.auth.domain.model;

import io.opencertflow.common.domain.error.BusinessException;
import java.util.regex.Pattern;

/**
 * 이메일 주소 값 객체. 저장·비교의 기준이 되는 정규화된 형태다.
 *
 * <p>생성 시 <b>소문자로 정규화</b>한다. {@code User@x.com}과 {@code user@x.com}이 다른 계정으로
 * 취급되면 같은 사람이 두 번 가입하거나 로그인에 실패한다. 정규화를 값 객체에 가두면 이 실수가
 * 시스템 어디에서도 재발하지 않는다.
 *
 * <p>형식 검증은 완벽한 RFC 5322 준수가 아니라 "명백히 잘못된 입력을 막는" 수준이다. 진짜 검증은
 * 이메일 인증 코드 발송으로 한다 — 형식만 맞고 존재하지 않는 주소는 인증 단계에서 걸러진다.
 */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_LENGTH = 254; // RFC 5321 실무 상한

    public Email {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid("이메일을 입력해 주세요.");
        }
        value = value.strip().toLowerCase();
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw BusinessException.invalid("이메일 형식이 올바르지 않습니다.");
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
