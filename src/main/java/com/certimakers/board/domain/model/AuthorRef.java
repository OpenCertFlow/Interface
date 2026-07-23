package com.certimakers.board.domain.model;

import com.certimakers.common.domain.model.Guard;
import java.util.UUID;

/**
 * 글·댓글 작성자에 대한 참조. 인증 컨텍스트의 사용자를 <b>식별자로만</b> 가리킨다.
 *
 * <p>표시용 닉네임을 여기 담지 않는 이유는, 닉네임이 바뀌면 과거 글의 표기가 모두 어긋나기
 * 때문이다. 표기는 조회 시점에 인증 컨텍스트에서 가져온다.
 */
public record AuthorRef(UUID value) {

    public AuthorRef {
        Guard.notNull(value, "authorId");
    }

    public static AuthorRef of(UUID value) {
        return new AuthorRef(value);
    }

    public static AuthorRef of(String value) {
        return new AuthorRef(UUID.fromString(value));
    }
}
