-- 공식 문서 원문이 바뀌었는지 감지하기 위한 컬럼.
--
-- 운영지침은 "공식 자료를 자동으로 항상 최신 상태로 갱신한다고 표현하지 않는다"로 못을 박았다.
-- 옳은 판단이지만 그 반대급부로 "그럼 원문이 바뀌면?"이 남는다. 자동 갱신은 하지 않되
-- 원문 해시를 견주어 **변경을 감지**해 관리자에게 재검토를 요청한다. 갱신 주체는 여전히 사람이다.
ALTER TABLE official_document
    ADD COLUMN content_hash        varchar(64),
    ADD COLUMN content_checked_at  timestamptz,
    ADD COLUMN change_detected_at  timestamptz;

COMMENT ON COLUMN official_document.content_hash IS '마지막으로 확인한 원문 본문의 SHA-256';
COMMENT ON COLUMN official_document.content_checked_at IS '원문을 마지막으로 가져와 본 시각';
COMMENT ON COLUMN official_document.change_detected_at IS '해시가 달라진 것을 감지한 시각. 재검토 후 비운다';
