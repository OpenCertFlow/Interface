-- 진단 입력 초안 (F-APP-004)
--
-- 저장해 두고 이어서 작성하는 미완성 제품 입력이다. 완성된 진단과 달리 구조를 강제하지 않고
-- 입력 원문(JSON)을 payload에 그대로 담는다 — 검증은 실제 진단 실행 시점에 한다.
-- 초안은 항상 소유자가 있다(로그인 필요).

CREATE TABLE diagnosis_draft (
    id            bigint PRIMARY KEY,
    owner_user_id varchar(40) NOT NULL,
    product_group varchar(40),
    payload       text        NOT NULL,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL
);

CREATE INDEX idx_diagnosis_draft_owner ON diagnosis_draft (owner_user_id, updated_at DESC);
