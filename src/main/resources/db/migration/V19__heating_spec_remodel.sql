-- 전기방석 입력 모델 재정의 (기능정의서 F-APP-014/016/017 정밀화)
--
-- 신체접촉 여부(boolean)를 접촉 방식(enum 4값)으로, 온도조절기 유무(boolean)를 상태(있음/없음/모름)로,
-- 표면온도에 출처(측정/추정/모름)를, 자동차단에 꺼짐 시간(분)을, 온도조절기에 조절 단계를 더한다.
--
-- 기존 컬럼(direct_body_contact·has_temperature_controller)은 남기되 더는 쓰지 않는다(과거 기록 보존,
-- 신규 행은 NULL). 그 컬럼을 참조하던 CHECK 제약은 제거한다 — 짝 규칙은 도메인 HeatingSpec이 강제한다.

ALTER TABLE product_profile DROP CONSTRAINT IF EXISTS ck_product_profile_heating_pair;
ALTER TABLE product_profile DROP CONSTRAINT IF EXISTS ck_product_profile_surface_temp;
ALTER TABLE product_profile DROP CONSTRAINT IF EXISTS ck_product_profile_heating_detail_scope;

ALTER TABLE product_profile
    ADD COLUMN body_contact_type     varchar(30),
    ADD COLUMN controller_status     varchar(20),
    ADD COLUMN adjustment_steps      int,
    ADD COLUMN temperature_source    varchar(20),
    ADD COLUMN auto_shut_off_minutes int;

COMMENT ON COLUMN product_profile.body_contact_type IS
    '신체 접촉 방식(DIRECT_SKIN·THROUGH_CLOTHING·THROUGH_COVER·NONE). 발열 제품이 아니면 NULL';
COMMENT ON COLUMN product_profile.controller_status IS
    '온도조절기 유무(PRESENT·ABSENT·UNKNOWN)';
COMMENT ON COLUMN product_profile.adjustment_steps IS
    '온도 조절 단계 수. 조절기가 있을 때만 값이 있다';
COMMENT ON COLUMN product_profile.temperature_source IS
    '표면온도 값의 출처(MEASURED·ESTIMATED·UNKNOWN)';
COMMENT ON COLUMN product_profile.auto_shut_off_minutes IS
    '자동으로 꺼지기까지의 시간(분). 자동 차단이 있을 때만 값이 있다';
