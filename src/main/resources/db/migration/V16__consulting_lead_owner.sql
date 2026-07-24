-- 상담 리드 소유자 연결 (알림 수신자·내 상담 조회의 기반, F-BE-013·F-APP-041)
--
-- 로그인한 소공인이 접수하면 그 사용자 id를 소유자로 남긴다. 비로그인 접수는 NULL(익명).

ALTER TABLE consulting_lead ADD COLUMN owner_user_id varchar(40);
CREATE INDEX idx_consulting_lead_owner ON consulting_lead (owner_user_id, created_at DESC);

COMMENT ON COLUMN consulting_lead.owner_user_id IS '접수한 로그인 사용자 id. 비로그인 접수면 NULL';
