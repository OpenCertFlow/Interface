-- CONSULTANT 역할 추가 (컨설턴트 웹 F-WCON, 컨설턴트 승인 F-WADM-003)
--
-- app_user.role CHECK 제약을 CONSULTANT까지 허용하도록 갱신한다. 기존 행(USER/ADMIN)은 영향 없다.

ALTER TABLE app_user DROP CONSTRAINT ck_app_user_role;
ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'CONSULTANT', 'ADMIN'));
