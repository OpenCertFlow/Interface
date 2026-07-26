-- 파일 공개/비공개 구분
--
-- board 첨부는 공개가 맞지만, document가 발급한 PDF와 비밀글 첨부는 소유자만 봐야 한다.
-- 지금까지 이 구분이 아예 없어서 전부 사실상 공개였다. 기본값은 PUBLIC으로 두고, 이미
-- 비공개였어야 할 두 가지 기존 데이터를 소급 전환한다.

ALTER TABLE stored_file
    ADD COLUMN visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';

ALTER TABLE stored_file
    ADD CONSTRAINT ck_stored_file_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'));

-- 이미 발급된 문서의 PDF는 소급으로 비공개 전환
UPDATE stored_file
SET visibility = 'PRIVATE'
WHERE id IN (SELECT file_id FROM issued_document);

-- 이미 비밀글에 첨부된 파일도 소급으로 비공개 전환
UPDATE stored_file
SET visibility = 'PRIVATE'
WHERE id IN (
    SELECT unnest(string_to_array(attachment_ids, ','))::bigint
    FROM board_post
    WHERE secret = true AND attachment_ids IS NOT NULL AND attachment_ids <> ''
);
