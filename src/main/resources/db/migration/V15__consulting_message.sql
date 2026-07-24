-- 상담 메시지 스레드 (기능정의서 F-WCON-008 추가정보 요청, F-WCON-009 공개 안내, F-WCON-011 이력)
--
-- 컨설턴트가 리드에 남기는 메시지. 공개(INFO_REQUEST·REPLY)는 소공인이 리드 id로 조회할 수 있고,
-- 내부 메모(NOTE)는 컨설턴트에게만 보인다.

CREATE TABLE consulting_message (
    id         uuid PRIMARY KEY,
    lead_id    uuid NOT NULL REFERENCES consulting_lead (id) ON DELETE CASCADE,
    author_id  varchar(40),
    kind       varchar(20)  NOT NULL,
    body       text         NOT NULL,
    created_at timestamptz  NOT NULL
);
CREATE INDEX idx_consulting_message_lead ON consulting_message (lead_id, created_at);

COMMENT ON COLUMN consulting_message.kind IS 'INFO_REQUEST·REPLY(공개) | NOTE(내부)';
COMMENT ON COLUMN consulting_message.author_id IS '작성 컨설턴트 id. 공개 조회에는 노출하지 않는다';
