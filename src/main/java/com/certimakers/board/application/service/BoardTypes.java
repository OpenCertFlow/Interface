package com.certimakers.board.application.service;

import com.certimakers.board.domain.model.BoardType;
import com.certimakers.common.domain.error.BusinessException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 게시판 종류 문자열 파싱. 잘못된 값은 사용자가 고칠 수 있는 메시지로 바꾼다.
 *
 * <p>선택 가능한 값을 오류 메시지에 함께 실어 준다 — 클라이언트가 무엇을 보내야 하는지 응답만 보고
 * 알 수 있어야 한다.
 */
final class BoardTypes {

    private BoardTypes() {
    }

    static BoardType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.invalid("게시판 종류가 필요합니다.");
        }
        try {
            return BoardType.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid(
                    "게시판 종류가 올바르지 않습니다. 가능한 값: %s".formatted(available()));
        }
    }

    private static String available() {
        return Arrays.stream(BoardType.values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
