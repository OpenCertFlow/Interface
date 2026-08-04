-- 컨설턴트가 근거의 적절성을 되먹인다.
--
-- 지금까지 근거 품질은 일방향이었다. 관리자가 색인하고 사용자가 본다. 그런데 그 근거가 제품에
-- 맞는지 가장 잘 아는 사람은 상담을 처리하는 컨설턴트다. 그 판단을 받아 두면 색인 재검토의
-- 우선순위가 생기고, 양면 구조가 데이터로 순환한다.
CREATE TABLE evidence_feedback (
    id                 bigint PRIMARY KEY,
    diagnosis_id       bigint      NOT NULL REFERENCES diagnosis (id) ON DELETE CASCADE,
    source_document_id varchar(120) NOT NULL,
    section_type       varchar(30),
    verdict            varchar(20) NOT NULL,   -- USEFUL · IRRELEVANT · OUTDATED · WRONG_PRODUCT
    comment            text,
    reported_by        varchar(64) NOT NULL,   -- 컨설턴트 계정
    created_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_evidence_feedback_document ON evidence_feedback (source_document_id);
CREATE INDEX idx_evidence_feedback_diagnosis ON evidence_feedback (diagnosis_id);
