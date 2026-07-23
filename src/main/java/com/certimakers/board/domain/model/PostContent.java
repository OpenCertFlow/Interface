package com.certimakers.board.domain.model;

import com.certimakers.common.domain.error.BusinessException;

/**
 * 게시글 제목과 본문.
 *
 * <p>둘을 한 값 객체로 묶은 이유는 항상 함께 검증되고 함께 수정되기 때문이다. 제목만 바꾸는 수정도
 * 결국 본문과 같은 규칙을 통과해야 한다.
 *
 * <p>HTML 이스케이프는 여기서 하지 않는다. 저장은 원문 그대로 하고, 출력하는 쪽(앱·웹)이 자기
 * 렌더링 방식에 맞게 처리하는 것이 옳다 — 저장 시점에 이스케이프하면 표현 방식이 바뀔 때
 * 이미 저장된 데이터를 되돌릴 수 없다.
 */
public record PostContent(String title, String body) {

    private static final int TITLE_MAX = 200;
    private static final int BODY_MAX = 20_000;

    public PostContent {
        if (title == null || title.isBlank()) {
            throw BusinessException.invalid("제목을 입력해 주세요.");
        }
        if (body == null || body.isBlank()) {
            throw BusinessException.invalid("내용을 입력해 주세요.");
        }
        title = title.strip();
        body = body.strip();
        if (title.length() > TITLE_MAX) {
            throw BusinessException.invalid("제목은 %d자 이하로 입력해 주세요.".formatted(TITLE_MAX));
        }
        if (body.length() > BODY_MAX) {
            throw BusinessException.invalid("내용은 %d자 이하로 입력해 주세요.".formatted(BODY_MAX));
        }
    }

    public static PostContent of(String title, String body) {
        return new PostContent(title, body);
    }
}
