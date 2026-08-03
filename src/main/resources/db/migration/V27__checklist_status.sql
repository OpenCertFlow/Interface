-- 체크리스트 항목의 '보유 여부(boolean)'를 '상태(HELD/MISSING/UNKNOWN)'로 확장한다.
--
-- 이유: 사용자가 "모른다"고 답한 서류를 "없다"로 저장하면 정보가 소실된다. 없는 것은 만들어야
-- 하고 모르는 것은 확인해야 하므로 다음 행동이 다르다(운영지침 §9 — 미확인 정보를 임의로
-- 정상·부적합으로 판정하지 않는다).
ALTER TABLE checklist_item ADD COLUMN status varchar(16);
UPDATE checklist_item SET status = CASE WHEN held THEN 'HELD' ELSE 'MISSING' END;
ALTER TABLE checklist_item ALTER COLUMN status SET NOT NULL;
ALTER TABLE checklist_item DROP COLUMN held;

-- 제품 프로필에도 '모름' 체크를 스냅샷으로 남긴다. 재진단·시뮬레이션이 원 입력을 그대로
-- 복원할 수 있어야 한다.
ALTER TABLE product_profile
    ADD COLUMN unknown_documents jsonb NOT NULL DEFAULT '[]'::jsonb;
