-- 발열 제품 상세 입력 추가 (기능정의서 F-APP-014~018, 전기방석 상세 입력)
--
-- 신체 접촉·온도조절기·표면온도(V8)만으로는 문서가 요구하는 판별을 할 수 없다. 의료적 표현 여부,
-- 자동 차단·과열 보호, 커버·세탁·전기부 분리, 별도 어댑터(외장/인증)를 추가한다.
--
-- 모두 nullable로 추가한다 — 기존 진단 기록은 발열 제품이 아니거나 이 상세를 받기 전의 입력이므로
-- NULL로 남는다. 진단은 그 시점의 입력을 그대로 보존해야 하는 기록이라 소급 채우지 않는다.

ALTER TABLE product_profile
    ADD COLUMN medical_use_claim           boolean,
    ADD COLUMN auto_shut_off               boolean,
    ADD COLUMN overheat_protection         boolean,
    ADD COLUMN removable_cover             boolean,
    ADD COLUMN washable                    boolean,
    ADD COLUMN separable_electric_parts    boolean,
    ADD COLUMN has_separate_adapter        boolean,
    ADD COLUMN adapter_externally_attached boolean,
    ADD COLUMN adapter_certified           boolean;

-- 어댑터 세부(외장 여부·인증 여부)는 별도 어댑터가 있을 때만 의미가 있다. 어댑터가 없으면 NULL,
-- 있으면 둘 다 있어야 한다 — "어댑터가 있는데 인증 여부를 모른다"는 모호한 상태를 DB에서도 막는다.
-- has_separate_adapter 자체가 NULL인 행(발열 제품이 아님)도 세부는 NULL이어야 한다.
ALTER TABLE product_profile
    ADD CONSTRAINT ck_product_profile_adapter_detail CHECK (
        (has_separate_adapter IS DISTINCT FROM TRUE
            AND adapter_externally_attached IS NULL
            AND adapter_certified IS NULL)
        OR (has_separate_adapter = TRUE
            AND adapter_externally_attached IS NOT NULL
            AND adapter_certified IS NOT NULL)
    );

-- 발열 상세는 발열 제품(direct_body_contact 존재)에서만 채워진다. 발열 제품이 아닌데 상세만
-- 채워진 상태는 있을 수 없다.
ALTER TABLE product_profile
    ADD CONSTRAINT ck_product_profile_heating_detail_scope CHECK (
        direct_body_contact IS NOT NULL
        OR (medical_use_claim IS NULL
            AND auto_shut_off IS NULL
            AND overheat_protection IS NULL
            AND removable_cover IS NULL
            AND washable IS NULL
            AND separable_electric_parts IS NULL
            AND has_separate_adapter IS NULL)
    );

COMMENT ON COLUMN product_profile.medical_use_claim IS
    '혈액순환·통증 완화 등 의료적 효능을 표방하는지. 표방하면 의료기기 규제 영역';
COMMENT ON COLUMN product_profile.auto_shut_off IS
    '일정 시간 뒤 자동 전원 차단 장치 유무';
COMMENT ON COLUMN product_profile.overheat_protection IS
    '과열 시 전원을 차단하는 온도 제한 장치 유무';
COMMENT ON COLUMN product_profile.removable_cover IS
    '커버 분리 가능 여부(세탁용)';
COMMENT ON COLUMN product_profile.washable IS
    '물세탁 가능 여부';
COMMENT ON COLUMN product_profile.separable_electric_parts IS
    '세탁 시 열선·컨트롤러 등 전기부 분리 가능 여부';
COMMENT ON COLUMN product_profile.has_separate_adapter IS
    '내장형이 아니라 별도 전원 어댑터 사용 여부';
COMMENT ON COLUMN product_profile.adapter_externally_attached IS
    '어댑터가 외장형인지(동봉/외장 구분). 어댑터가 없으면 NULL';
COMMENT ON COLUMN product_profile.adapter_certified IS
    '어댑터 자체의 KC 등 인증 보유 여부. 어댑터가 없으면 NULL';
