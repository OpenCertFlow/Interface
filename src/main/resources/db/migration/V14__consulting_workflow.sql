-- 컨설팅 워크플로 필드 추가 (기능정의서 F-WCON 컨설턴트 상담 처리)
--
-- 담당 컨설턴트와 내부 메모를 추가한다. 기존 리드는 미배정(NULL)으로 남는다.

ALTER TABLE consulting_lead
    ADD COLUMN assigned_consultant_id varchar(40),
    ADD COLUMN internal_memo          text;

COMMENT ON COLUMN consulting_lead.assigned_consultant_id IS '담당 컨설턴트 사용자 id. 미배정이면 NULL';
COMMENT ON COLUMN consulting_lead.internal_memo IS '컨설턴트 내부 메모. 사용자에게 공개되지 않는다';
