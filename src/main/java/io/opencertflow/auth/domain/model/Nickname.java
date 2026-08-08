package io.opencertflow.auth.domain.model;

import io.opencertflow.common.domain.error.BusinessException;

/**
 * 표시 이름 값 객체. 마이페이지·게시판 작성자 표기에 쓰인다.
 *
 * <p>앞뒤 공백을 제거하고 길이를 강제한다. 화면에 그대로 노출되므로 빈 문자열이나 지나치게 긴
 * 값이 저장되면 UI가 깨진다.
 */
public record Nickname(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 20;

    public Nickname {
        if (value == null || value.isBlank()) {
            throw BusinessException.invalid("닉네임을 입력해 주세요.");
        }
        value = value.strip();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw BusinessException.invalid(
                    "닉네임은 %d자 이상 %d자 이하로 입력해 주세요.".formatted(MIN_LENGTH, MAX_LENGTH));
        }
    }

    public static Nickname of(String value) {
        return new Nickname(value);
    }
}
