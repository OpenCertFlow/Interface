-- 진단 소유자 연결 (진단 이력 F-APP-032~035)
--
-- 진단은 기본적으로 익명이다(개인정보 최소수집). 다만 로그인 상태로 요청하면 owner_user_id가 채워져
-- '내 진단 이력' 조회·재진단·삭제의 기준이 된다. 연락처 등 개인정보는 여전히 저장하지 않는다.
-- nullable이며, 이전(익명) 진단 행은 null로 남는다.

ALTER TABLE diagnosis ADD COLUMN owner_user_id varchar(40);
CREATE INDEX idx_diagnosis_owner ON diagnosis (owner_user_id, created_at DESC);
