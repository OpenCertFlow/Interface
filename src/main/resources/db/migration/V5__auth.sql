-- 인증 컨텍스트: 사용자 계정
--
-- 테이블명이 app_user인 이유는 user가 PostgreSQL 예약어이기 때문이다.
-- password_hash는 소셜 계정(카카오)이면 NULL이고, provider_id는 로컬 계정이면 NULL이다.

CREATE TABLE app_user (
    id             BIGINT         PRIMARY KEY,
    email          VARCHAR(254) NOT NULL,
    password_hash  VARCHAR(100),
    nickname       VARCHAR(20)  NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    provider       VARCHAR(20)  NOT NULL,
    provider_id    VARCHAR(64),
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_app_user_provider CHECK (provider IN ('LOCAL', 'KAKAO')),
    -- 로컬 계정은 비밀번호가 있어야 하고, 소셜 계정은 provider_id가 있어야 한다.
    -- 두 상태가 뒤섞이면 로그인 경로가 모호해지므로 DB에서 막는다.
    CONSTRAINT ck_app_user_credential CHECK (
        (provider = 'LOCAL' AND password_hash IS NOT NULL)
        OR (provider <> 'LOCAL' AND provider_id IS NOT NULL)
    )
);

-- 같은 소셜 계정이 두 번 생성되지 않도록 (provider, provider_id) 유일성을 보장한다.
CREATE UNIQUE INDEX uk_app_user_provider_id
    ON app_user (provider, provider_id)
    WHERE provider_id IS NOT NULL;

COMMENT ON TABLE  app_user                IS '사용자 계정 (로컬·카카오)';
COMMENT ON COLUMN app_user.password_hash  IS 'BCrypt 해시. 소셜 계정이면 NULL';
COMMENT ON COLUMN app_user.provider_id    IS '소셜 제공자의 회원번호. 로컬 계정이면 NULL';
COMMENT ON COLUMN app_user.email_verified IS '이메일 인증 완료 여부. 카카오는 가입 시 TRUE';
