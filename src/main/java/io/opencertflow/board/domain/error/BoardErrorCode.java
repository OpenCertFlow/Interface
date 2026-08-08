package io.opencertflow.board.domain.error;

import io.opencertflow.common.domain.error.ErrorCode;
import io.opencertflow.common.domain.error.ErrorType;

/** 게시판 컨텍스트 고유 오류. {@code CM-BOARD-<번호>}. */
public enum BoardErrorCode implements ErrorCode {

    POST_NOT_FOUND("OCF-BOARD-001", "게시글을 찾을 수 없습니다.", ErrorType.NOT_FOUND),

    COMMENT_NOT_FOUND("OCF-BOARD-002", "댓글을 찾을 수 없습니다.", ErrorType.NOT_FOUND),

    /** 관리자 전용 게시판에 일반 회원이 글을 쓰려 했다. */
    ADMIN_ONLY_BOARD("OCF-BOARD-003", "이 게시판에는 관리자만 글을 쓸 수 있습니다.", ErrorType.CONFLICT),

    /** 댓글을 허용하지 않는 게시판이다. */
    COMMENTS_NOT_ALLOWED("OCF-BOARD-004", "이 게시판에는 댓글을 쓸 수 없습니다.", ErrorType.CONFLICT),

    /** 첨부를 허용하지 않는 게시판이다. */
    ATTACHMENTS_NOT_ALLOWED("OCF-BOARD-005", "이 게시판에는 파일을 첨부할 수 없습니다.", ErrorType.CONFLICT),

    /** 비밀글을 허용하지 않는 게시판이다. */
    SECRET_POST_NOT_ALLOWED("OCF-BOARD-006", "이 게시판에는 비밀글을 쓸 수 없습니다.", ErrorType.CONFLICT),

    /** 작성자·관리자가 아닌 사람이 수정·삭제를 시도했다. */
    NOT_POST_AUTHOR("OCF-BOARD-007", "본인이 작성한 글만 수정·삭제할 수 있습니다.", ErrorType.CONFLICT),

    NOT_COMMENT_AUTHOR("OCF-BOARD-008", "본인이 작성한 댓글만 수정·삭제할 수 있습니다.", ErrorType.CONFLICT),

    /** 비밀글을 작성자·관리자가 아닌 사람이 열람하려 했다. */
    SECRET_POST_FORBIDDEN("OCF-BOARD-009", "비밀글은 작성자와 관리자만 볼 수 있습니다.", ErrorType.CONFLICT),

    /** 첨부 개수 상한을 넘었다. */
    TOO_MANY_ATTACHMENTS("OCF-BOARD-010", "첨부할 수 있는 파일 개수를 초과했습니다.", ErrorType.VALIDATION),

    /** 본인 소유가 아닌 파일을 첨부하려 했다. */
    ATTACHMENT_NOT_OWNED("OCF-BOARD-011", "본인이 소유한 파일만 첨부할 수 있습니다.", ErrorType.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final ErrorType type;

    BoardErrorCode(String code, String defaultMessage, ErrorType type) {
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
