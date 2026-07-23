-- 발열 사양 추가 (전기방석 제품군 지원)
--
-- 기존 진단 기록은 발열 제품이 아니므로 세 컬럼 모두 NULL로 남는다. nullable로 추가해
-- 기존 행을 건드리지 않는다 — 진단은 그 시점의 입력을 그대로 보존해야 하는 기록이다.

ALTER TABLE product_profile
    ADD COLUMN direct_body_contact        boolean,
    ADD COLUMN has_temperature_controller boolean,
    ADD COLUMN max_surface_temperature    int;

-- 두 불리언은 함께 있거나 함께 없어야 한다.
-- 하나만 채워지면 "발열 제품인데 접촉 여부를 모른다"는 모호한 상태가 되고,
-- 룰이 그 상태를 어떻게 다룰지 정의되어 있지 않다.
ALTER TABLE product_profile
    ADD CONSTRAINT ck_product_profile_heating_pair CHECK (
        (direct_body_contact IS NULL AND has_temperature_controller IS NULL)
        OR (direct_body_contact IS NOT NULL AND has_temperature_controller IS NOT NULL)
    );

-- 표면온도만 있고 발열 제품이 아닌 상태는 있을 수 없다.
ALTER TABLE product_profile
    ADD CONSTRAINT ck_product_profile_surface_temp CHECK (
        max_surface_temperature IS NULL
        OR direct_body_contact IS NOT NULL
    );

-- 사람이 접촉하는 제품에서 현실적으로 나올 수 있는 범위. 입력 오타를 DB에서도 막는다.
ALTER TABLE product_profile
    ADD CONSTRAINT ck_product_profile_surface_temp_range CHECK (
        max_surface_temperature IS NULL
        OR (max_surface_temperature BETWEEN 0 AND 300)
    );

COMMENT ON COLUMN product_profile.direct_body_contact IS
    '사용 중 신체에 직접 닿는지. 발열 제품이 아니면 NULL';
COMMENT ON COLUMN product_profile.has_temperature_controller IS
    '온도조절기(과열 방지 장치) 유무. 발열 제품이 아니면 NULL';
COMMENT ON COLUMN product_profile.max_surface_temperature IS
    '최고 표면온도(℃). 발열 제품이어도 측정하지 않았으면 NULL — 판단 불가로 이어진다';
