-- 알림 (기능정의서 F-BE-013 진단·상담 알림, F-APP-045 상태 알림)
--
-- 수신자(로그인 사용자)에게 상담 상태 변경·공개 메시지 등을 알린다. 익명 리드(소유자 없음)는
-- 수신자가 없어 알림이 생성되지 않는다.

CREATE TABLE notification (
    id                uuid PRIMARY KEY,
    recipient_user_id varchar(40)  NOT NULL,
    kind              varchar(40)  NOT NULL,
    title             varchar(200) NOT NULL,
    body              text,
    ref_type          varchar(40),
    ref_id            varchar(64),
    is_read           boolean      NOT NULL DEFAULT false,
    created_at        timestamptz  NOT NULL
);
CREATE INDEX idx_notification_recipient ON notification (recipient_user_id, created_at DESC);
CREATE INDEX idx_notification_unread ON notification (recipient_user_id, is_read);
