-- 문서 발급 컨텍스트
--
-- 입력값은 JSON으로 담는다. 양식마다 항목이 다르므로 컬럼으로 펴면 양식을 추가할 때마다
-- 스키마 변경이 필요하다. 값은 재발급 시 다시 채워 주는 용도로만 읽으므로 JSON으로 충분하다.
--
-- file_id에 외래키를 걸지 않는 이유는 파일이 다른 바운디드 컨텍스트이기 때문이다(V6와 같은 판단).

CREATE TABLE issued_document (
    id            BIGINT        PRIMARY KEY,
    template_code VARCHAR(40) NOT NULL,
    values_json   TEXT        NOT NULL,
    issuer_id     BIGINT        NOT NULL,
    file_id       BIGINT        NOT NULL,
    issued_at     TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_issued_document_template CHECK (
        template_code IN ('SELF_DECLARATION', 'PRODUCT_SPEC', 'SAFETY_LABEL_PLAN', 'TEST_REQUEST')
    )
);

-- 발급 이력 조회는 항상 '내 문서를 최신순으로'다.
CREATE INDEX idx_issued_document_issuer ON issued_document (issuer_id, issued_at DESC);

COMMENT ON TABLE  issued_document             IS '발급된 문서 이력. PDF 자체는 파일 컨텍스트에 저장된다';
COMMENT ON COLUMN issued_document.values_json IS '양식에 채운 값(JSON). 재발급 시 다시 채워 주는 용도';
COMMENT ON COLUMN issued_document.file_id     IS '생성된 PDF의 stored_file 식별자. 외래키는 걸지 않는다';
