-- V3: 진단 애그리거트. 한 트랜잭션에서 통째로 저장·조회된다. docs/design/05-data-model.md 참조.

CREATE TABLE diagnosis (
    id                  uuid PRIMARY KEY,                 -- 애플리케이션이 UUIDv7 생성
    status              varchar(30) NOT NULL,
    rule_set_id         uuid,
    rule_set_version    int,                              -- 재현용 스냅샷 (FK와 별개로 중복 저장)
    readiness_score     int,                              -- 산정 불가면 null
    score_applicable    boolean     NOT NULL DEFAULT false,
    earned_weight       int         NOT NULL DEFAULT 0,
    total_weight        int         NOT NULL DEFAULT 0,
    degraded_evidence   boolean     NOT NULL DEFAULT false,
    degraded_narration  boolean     NOT NULL DEFAULT false,
    created_at          timestamptz NOT NULL,
    updated_at          timestamptz NOT NULL
);
CREATE INDEX idx_diagnosis_created_at ON diagnosis (created_at DESC);

-- 제품 프로파일 스냅샷. 진단과 1:1.
CREATE TABLE product_profile (
    diagnosis_id      uuid PRIMARY KEY REFERENCES diagnosis (id) ON DELETE CASCADE,
    product_name      varchar(200) NOT NULL,
    product_group     varchar(40)  NOT NULL,
    uses_electricity  boolean      NOT NULL,
    rated_voltage     int,
    power_consumption int,
    has_battery       boolean      NOT NULL,
    target_user       varchar(30)  NOT NULL,
    sales_channel     varchar(30)  NOT NULL,
    materials         jsonb        NOT NULL,
    held_documents    jsonb        NOT NULL
);

CREATE TABLE certification_candidate (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    diagnosis_id       uuid NOT NULL REFERENCES diagnosis (id) ON DELETE CASCADE,
    scheme_code        varchar(80) NOT NULL,
    certification_type varchar(40) NOT NULL,
    matched_rule_codes jsonb       NOT NULL
);
CREATE INDEX idx_candidate_diagnosis ON certification_candidate (diagnosis_id);

CREATE TABLE checklist_item (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    diagnosis_id  uuid NOT NULL REFERENCES diagnosis (id) ON DELETE CASCADE,
    document_code varchar(60) NOT NULL,
    requirement   varchar(20) NOT NULL,
    weight        int         NOT NULL,          -- 평가 시점 가중치 스냅샷
    held          boolean     NOT NULL
);
CREATE INDEX idx_checklist_diagnosis ON checklist_item (diagnosis_id);

CREATE TABLE labeling_check_item (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    diagnosis_id       uuid NOT NULL REFERENCES diagnosis (id) ON DELETE CASCADE,
    label              text  NOT NULL,
    matched_rule_codes jsonb NOT NULL
);
CREATE INDEX idx_labeling_diagnosis ON labeling_check_item (diagnosis_id);

CREATE TABLE expert_review_item (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    diagnosis_id uuid NOT NULL REFERENCES diagnosis (id) ON DELETE CASCADE,
    question     text        NOT NULL,
    reason       varchar(40) NOT NULL
);
CREATE INDEX idx_expert_diagnosis ON expert_review_item (diagnosis_id);

CREATE TABLE diagnosis_evidence (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    diagnosis_id      uuid NOT NULL REFERENCES diagnosis (id) ON DELETE CASCADE,
    source_document_id varchar(120) NOT NULL,
    section_type      varchar(40)  NOT NULL,
    snippet           text         NOT NULL,
    source_url        text         NOT NULL,     -- 불변식 6: 출처 없는 근거는 없다
    relevance         double precision NOT NULL
);
CREATE INDEX idx_evidence_diagnosis ON diagnosis_evidence (diagnosis_id);

-- 설명 문장. 진단과 0:1.
CREATE TABLE narration (
    diagnosis_id          uuid PRIMARY KEY REFERENCES diagnosis (id) ON DELETE CASCADE,
    summary               text        NOT NULL,
    next_actions          jsonb       NOT NULL,
    pre_consult_questions jsonb       NOT NULL,
    disclaimer            text        NOT NULL,
    model_id              varchar(80) NOT NULL,
    is_template_fallback  boolean     NOT NULL
);
