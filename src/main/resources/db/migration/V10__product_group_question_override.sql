-- 제품군 질문(입력 스키마) 프레젠테이션 오버라이드 (기능정의서 F-WADM-006~008 질문 관리)
--
-- 입력 항목의 코드·타입·의존(dependsOn)은 진단 요청 DTO·Attribute enum·룰과 묶인 "타입 계약"이라
-- 코드에 남긴다 — 결정론적 룰 엔진이 읽을 수 없는 임의 질문이 생기면 안 되기 때문이다.
--
-- 이 테이블은 그 계약 위에 얹는 **프레젠테이션 오버라이드**만 담는다: 라벨·도움말·필수 여부·표시
-- 순서·노출 여부·선택 보기. 값이 없으면(NULL) enum의 기본값을 그대로 쓴다. 즉 오버라이드가 하나도
-- 없으면 기존 동작과 완전히 동일하다 — 시드가 필요 없고, 조회가 비는 일도 없다.

CREATE TABLE product_group_question_override (
    id            bigint PRIMARY KEY,
    product_group varchar(40) NOT NULL,
    code          varchar(60) NOT NULL,
    label         varchar(200),
    help_text     text,
    required      boolean,
    display_order int,
    active        boolean     NOT NULL DEFAULT true,
    -- 선택형 항목의 보기 오버라이드. NULL이면 enum 기본 보기를 쓴다.
    -- [{"value":"...","label":"..."}] 형태의 JSON 배열.
    options_json  jsonb,
    updated_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (product_group, code)
);

COMMENT ON TABLE product_group_question_override IS
    '제품군 입력 항목의 프레젠테이션 오버라이드. 코드/타입/의존은 코드 계약이라 여기서 다루지 않는다';
COMMENT ON COLUMN product_group_question_override.required IS
    'NULL이면 enum 기본값. 값이 있으면 필수 여부를 덮어쓴다';
COMMENT ON COLUMN product_group_question_override.active IS
    'false면 해당 항목을 입력 화면에서 숨긴다(코드 계약은 남지만 노출하지 않음)';
