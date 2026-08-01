-- 재진단 부모 연결 (재진단 결과 비교 F-APP-048)
--
-- 재진단은 원 진단의 입력을 복사해 새 진단을 만들지만, 지금은 '어느 진단의 재진단인지'가
-- 어디에도 남지 않아 두 진단을 비교할 수 없다. previous_id로 그 관계를 기록한다.
-- diagnosis가 diagnosis를 가리키는 자기참조 FK이며, 최초 진단과 기존 행은 모두 null이다.
--
-- ON DELETE SET NULL: 원 진단을 삭제해도 재진단은 그 자체로 유효한 진단이므로 남긴다.
-- CASCADE면 원본 삭제 시 최신 재진단까지 사라지고, RESTRICT면 원본 삭제가 500으로 막힌다.
-- null은 도메인에서 이미 '부모 없음 = 비교 대상 없음'으로 다루므로 추가 분기가 필요 없다.

ALTER TABLE diagnosis ADD COLUMN previous_id bigint
    REFERENCES diagnosis (id) ON DELETE SET NULL;

-- Postgres는 참조하는 쪽 컬럼에 인덱스를 자동 생성하지 않는다. 인덱스가 없으면 진단을 삭제할
-- 때마다 SET NULL 대상을 찾느라 diagnosis 전체를 순차 스캔한다.
CREATE INDEX idx_diagnosis_previous ON diagnosis (previous_id);