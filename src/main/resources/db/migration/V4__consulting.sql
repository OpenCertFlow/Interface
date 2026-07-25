-- V4: 컨설팅 리드 · 동의 로그. docs/design/05-data-model.md 참조.
-- 개인정보는 이 두 테이블에만 존재한다 — 진단 자체는 익명이다(개인정보 최소수집).

CREATE TABLE consulting_lead (
    id            bigint PRIMARY KEY,                    -- 애플리케이션이 전역 시퀀스로 생성
    diagnosis_id  bigint NOT NULL REFERENCES diagnosis (id),
    contact_name  varchar(100) NOT NULL,
    contact_phone text         NOT NULL,               -- AES-GCM 암호화 후 Base64
    contact_email text,                                -- AES-GCM 암호화 후 Base64 (선택)
    message       text,
    status        varchar(20)  NOT NULL,
    created_at    timestamptz  NOT NULL
);
CREATE INDEX idx_consulting_lead_status ON consulting_lead (status, created_at DESC);
CREATE INDEX idx_consulting_lead_diagnosis ON consulting_lead (diagnosis_id);

CREATE TABLE consent_log (
    id                         bigint PRIMARY KEY,
    consulting_lead_id         bigint NOT NULL REFERENCES consulting_lead (id) ON DELETE CASCADE,
    diagnosis_id               bigint NOT NULL,
    privacy_consent            boolean     NOT NULL,
    sensitive_info_consent     boolean     NOT NULL,
    service_limit_acknowledged boolean     NOT NULL,
    consent_version            varchar(40) NOT NULL,
    consented_at               timestamptz NOT NULL
);
CREATE INDEX idx_consent_log_diagnosis ON consent_log (diagnosis_id);
