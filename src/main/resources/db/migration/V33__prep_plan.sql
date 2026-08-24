-- 인증 준비 트래커 (F-APP-049)
--
-- 진단이 알려준 누락 서류를 사용자가 하나씩 확보하며 체크하는 목록이다. 진단은 평가 시점의
-- 스냅샷이라 건드리지 않고, 진단을 참조하는 별도 애그리거트로 둔다.
-- 준비도 점수는 재계산하지 않는다(PM 검토) — 완료 건수 대비 진행률만 보여준다.
-- CertiMakers/BackEnd#53(V31)의 포팅. V31·V32가 선점되어 V33으로 재번호.

CREATE TABLE prep_plan (
    id            bigint      PRIMARY KEY,             -- 애플리케이션이 전역 시퀀스로 생성
    owner_user_id varchar(40) NOT NULL,                -- diagnosis.owner_user_id와 같은 타입
    diagnosis_id  bigint      NOT NULL REFERENCES diagnosis (id) ON DELETE CASCADE,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL,
    -- 진단 하나에 계획 하나. 중복 생성을 DB가 막는다.
    CONSTRAINT uk_prep_plan_diagnosis UNIQUE (diagnosis_id)
);

CREATE TABLE prep_plan_item (
    id            bigint      PRIMARY KEY,
    prep_plan_id  bigint      NOT NULL REFERENCES prep_plan (id) ON DELETE CASCADE,
    document_code varchar(40) NOT NULL,
    done          boolean     NOT NULL DEFAULT false,
    CONSTRAINT uk_prep_item_code UNIQUE (prep_plan_id, document_code)
);

CREATE INDEX idx_prep_plan_owner ON prep_plan (owner_user_id, created_at DESC);
