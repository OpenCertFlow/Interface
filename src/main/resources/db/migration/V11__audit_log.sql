-- 감사 로그 (기능정의서 F-BE-018 중요행위 기록, F-WADM-018 조회)
--
-- 관리자 변경 행위(룰·질문·가중치·문서·권한 편집)를 누가·언제·무엇을 했는지 남긴다.
-- 웹 필터가 /api/v1/admin/** 의 변경 요청(POST/PUT/PATCH/DELETE)을 자동 기록한다.

CREATE TABLE audit_log (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor        varchar(100) NOT NULL,
    http_method  varchar(10)  NOT NULL,
    request_path varchar(500) NOT NULL,
    status_code  int,
    occurred_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_log_actor ON audit_log (actor, occurred_at DESC);

COMMENT ON COLUMN audit_log.actor IS '행위자(인증된 사용자 id). 미인증이면 anonymous';
