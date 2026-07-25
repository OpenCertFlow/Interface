-- 약관·동의 (기능정의서 F-AUTH-008 필수·선택 약관 동의)
--
-- 약관은 버전으로 관리하고, 회원가입 시 어떤 약관에 동의했는지 기록한다. 필수 약관에 동의하지
-- 않으면 가입이 거부된다(서비스 계층이 강제).

CREATE TABLE terms (
    id         uuid PRIMARY KEY,
    term_key   varchar(40)  NOT NULL,
    version    varchar(20)  NOT NULL,
    title      varchar(200) NOT NULL,
    content    text         NOT NULL,
    required   boolean      NOT NULL,
    active     boolean      NOT NULL DEFAULT true,
    created_at timestamptz  NOT NULL DEFAULT now(),
    UNIQUE (term_key, version)
);
-- 약관 종류당 활성 버전은 하나다.
CREATE UNIQUE INDEX uq_terms_active_key ON terms (term_key) WHERE active;

CREATE TABLE user_terms_agreement (
    id           uuid PRIMARY KEY,
    user_id      uuid        NOT NULL,
    term_key     varchar(40) NOT NULL,
    term_version varchar(20) NOT NULL,
    agreed_at    timestamptz NOT NULL
);
CREATE INDEX idx_user_terms_agreement_user ON user_terms_agreement (user_id);
