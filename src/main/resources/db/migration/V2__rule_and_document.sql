-- V2: 룰 · 공식 문서 · 가중치 기준표. docs/design/05-data-model.md 참조.

-- 공식 문서 메타데이터. RAG 워커의 수집 파이프라인이 채우며, 백엔드는 룰의 근거 참조로만 읽는다.
CREATE TABLE official_document (
    id                 bigint PRIMARY KEY DEFAULT nextval('global_id_seq'),
    title              varchar(300) NOT NULL,
    issuer             varchar(200) NOT NULL,
    published_at       date,
    verified_at        date,
    product_group      varchar(40)  NOT NULL,
    certification_type varchar(40),
    scheme_name        varchar(200),
    source_url         text         NOT NULL,
    created_at         timestamptz  NOT NULL DEFAULT now()
);

-- 문서 청크. 원문 텍스트의 진실의 원천이며, Qdrant는 이 청크의 검색 인덱스일 뿐이다.
-- 백엔드는 이 테이블을 매핑하지 않는다(RAG 워커가 채운다). id는 DB가 전역 시퀀스로 채워 준다.
-- qdrant_point_id는 우리 시퀀스가 아니라 Qdrant가 발급하는 외부 식별자라 uuid를 유지한다.
CREATE TABLE document_chunk (
    id              bigint PRIMARY KEY DEFAULT nextval('global_id_seq'),
    document_id     bigint NOT NULL REFERENCES official_document (id) ON DELETE CASCADE,
    section_type    varchar(40) NOT NULL,
    content         text        NOT NULL,
    seq             int         NOT NULL,
    qdrant_point_id uuid,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_document_chunk_doc_seq ON document_chunk (document_id, seq);

-- 룰셋. 제품군별로 버전된다. 부분 유니크 인덱스로 "제품군당 활성 룰셋 하나"를 강제한다.
CREATE TABLE rule_set (
    id            bigint PRIMARY KEY,
    version       int          NOT NULL,
    product_group varchar(40)  NOT NULL,
    active        boolean      NOT NULL DEFAULT false,
    activated_at  timestamptz,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    UNIQUE (product_group, version)
);
CREATE UNIQUE INDEX uq_rule_set_active_per_group
    ON rule_set (product_group) WHERE active;

-- 룰. condition과 effects를 jsonb로 저장한다. 코덱(RuleJsonCodec)이 도메인 트리로 되돌린다.
CREATE TABLE rule (
    id                 bigint PRIMARY KEY DEFAULT nextval('global_id_seq'),
    rule_set_id        bigint NOT NULL REFERENCES rule_set (id) ON DELETE CASCADE,
    rule_code          varchar(40) NOT NULL,
    priority           int         NOT NULL,
    condition          jsonb       NOT NULL,
    effects            jsonb       NOT NULL,
    description        text,
    source_document_id bigint REFERENCES official_document (id),
    created_at         timestamptz NOT NULL DEFAULT now(),
    UNIQUE (rule_set_id, rule_code)
);
CREATE INDEX idx_rule_set_priority ON rule (rule_set_id, priority);

-- 준비도 점수 가중치 기준표. 코드가 아니라 데이터로 관리한다(심사에서 근거 요구).
CREATE TABLE document_weight (
    document_code varchar(60) PRIMARY KEY,
    display_name  varchar(200) NOT NULL,
    requirement   varchar(20)  NOT NULL,
    weight        int          NOT NULL CHECK (weight > 0),
    note          text
);
