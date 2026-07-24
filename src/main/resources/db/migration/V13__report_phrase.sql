-- 리포트 문구 관리 (기능정의서 F-WADM-016)
--
-- 리포트에 노출되는 안내·면책 문구를 코드 배포 없이 관리한다. key로 조회하며, 값이 없으면
-- 클라이언트/리포트 생성부가 코드 기본값을 쓴다.

CREATE TABLE report_phrase (
    phrase_key  varchar(60) PRIMARY KEY,
    text        text        NOT NULL,
    description varchar(200),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
