-- 파일·게시판 컨텍스트
--
-- 두 컨텍스트를 한 마이그레이션에 담는 이유는 게시글 첨부가 파일을 참조하기 때문이다.
-- 다만 외래키는 걸지 않는다 — 바운디드 컨텍스트가 다르고, 파일이 지워져도 게시글은 남아야 한다.
-- 참조 무결성 대신 조회 시점에 '없는 첨부는 목록에서 제외'하는 방식으로 다룬다.

CREATE TABLE stored_file (
    id            UUID         PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(255) NOT NULL,
    size_in_bytes BIGINT       NOT NULL,
    storage_key   VARCHAR(255) NOT NULL,
    owner_id      UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_stored_file_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_stored_file_size CHECK (size_in_bytes > 0)
);

CREATE INDEX idx_stored_file_owner ON stored_file (owner_id, created_at DESC);

COMMENT ON TABLE  stored_file             IS '업로드된 파일의 메타데이터. 바이트는 저장소에 있다';
COMMENT ON COLUMN stored_file.storage_key IS '저장소 내 위치. 서버가 생성하며 사용자 입력이 섞이지 않는다';

CREATE TABLE board_post (
    id             UUID         PRIMARY KEY,
    board_type     VARCHAR(20)  NOT NULL,
    author_id      UUID         NOT NULL,
    title          VARCHAR(200) NOT NULL,
    body           TEXT         NOT NULL,
    secret         BOOLEAN      NOT NULL DEFAULT FALSE,
    attachment_ids VARCHAR(255),
    view_count     BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_board_post_type CHECK (board_type IN ('NOTICE', 'FREE', 'QNA', 'ARCHIVE')),
    CONSTRAINT ck_board_post_view_count CHECK (view_count >= 0)
);

-- 목록 조회는 항상 '게시판별 + 최신순'이다. 복합 인덱스로 정렬까지 인덱스에서 끝낸다.
CREATE INDEX idx_board_post_type_created ON board_post (board_type, created_at DESC);
CREATE INDEX idx_board_post_author ON board_post (author_id);

COMMENT ON TABLE  board_post                IS '게시글. 게시판 종류별 정책은 애플리케이션이 강제한다';
COMMENT ON COLUMN board_post.attachment_ids IS '첨부 파일 식별자를 쉼표로 이어 붙인 값. 최대 5개';

CREATE TABLE board_comment (
    id         UUID        PRIMARY KEY,
    post_id    UUID        NOT NULL,
    author_id  UUID        NOT NULL,
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    -- 댓글은 글과 다른 애그리거트지만 같은 컨텍스트다. 글이 지워지면 댓글도 함께 사라져야 한다.
    CONSTRAINT fk_board_comment_post FOREIGN KEY (post_id)
        REFERENCES board_post (id) ON DELETE CASCADE
);

CREATE INDEX idx_board_comment_post ON board_comment (post_id, created_at);

COMMENT ON TABLE board_comment IS '댓글. 글과 별도 애그리거트이며 post_id로만 참조한다';
