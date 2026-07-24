-- ============================================================================
-- 전기방석 룰셋 v1 — 뼈대 시드 (공식 확인 전)
-- ============================================================================
--
-- ⚠️ 이 룰셋은 아직 "무엇이 필요한지" 단정하지 않는다.
--
-- 전기방석의 인증 등급(안전인증/안전확인/공급자적합성)과 요구 서류는 공식 자료로 확인하기 전까지
-- 여기에 적지 않는다. 확인되지 않은 값을 넣으면 시연에서 **틀린 답을 자신 있게** 말하게 되고,
-- 그것은 이 서비스가 하지 않기로 한 일이다(불변식 6: 출처 없는 근거는 근거가 아니다).
--
-- 대신 지금은 조건 분기를 실제로 동작시키되, 결론을 '전문가 확인 필요'로 보낸다.
-- 이는 룰이 판단할 수 없을 때의 정상 동작이며(불변식 5), 시연에서도 정직하게 보인다.
--
-- ── 확인 후 해야 할 일 ──────────────────────────────────────────────────────
--   1. safetykorea.kr 품목 분류에서 '전기방석/전기요' 인증 등급 확인
--   2. 해당 등급의 요구 서류 목록 확인
--   3. 아래 [TODO-확인후] 표시된 룰의 flagExpertReview를 addCandidate + requireDocument로 교체
--   4. R-EH-001의 scheme_code / certification_type을 확인된 값으로 지정
--   5. document_weight 시드에 새 서류 코드의 가중치 추가 (R__seed_document_weight.sql)
--
-- JSON 형식은 RuleJsonCodec이 파싱하는 형식과 정확히 일치해야 한다(RuleJsonCodecTest가 검증).
-- ============================================================================

DELETE FROM rule_set WHERE product_group = 'ELECTRIC_HEATING_PAD' AND version = 1;

INSERT INTO rule_set (id, version, product_group, active, activated_at)
VALUES ('00000000-0000-0000-0000-000000000002', 1, 'ELECTRIC_HEATING_PAD', true, now());

-- ── R-EH-001: 전기 사용 + 신체 직접 접촉 → 발열 제품 식별 ───────────────────
-- [TODO-확인후] 인증 등급이 확인되면 flagExpertReview를 아래로 교체:
--   {"type":"addCandidate","schemeCode":"<확인된_제도코드>","certificationType":"<확인된_등급>"},
--   {"type":"requireDocument","documentCode":"...","requirement":"REQUIRED"}
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-001', 10,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"USES_ELECTRICITY","operator":"EQ","value":true},
     {"type":"attr","attribute":"DIRECT_BODY_CONTACT","operator":"EQ","value":true}
   ]}',
 '[
     {"type":"requireDocument","documentCode":"BIZ_LICENSE","requirement":"REQUIRED"},
     {"type":"flagExpertReview","question":"신체에 직접 닿는 발열 제품입니다. 적용되는 인증 제도와 등급을 인증기관에 확인해 주세요. (서비스에서 공식 기준 확인 후 자동 안내 예정)","reason":"NO_EVIDENCE"}
   ]',
 '전기 사용 + 신체 접촉 발열 제품 식별. 등급·서류는 공식 확인 전이라 전문가 확인으로 보낸다');

-- ── R-EH-002: 온도조절기 없음 → 과열 위험 확인 필요 ──────────────────────────
-- 온도조절기 유무가 시험 항목을 가르는 것은 발열 제품의 일반적 특성이나,
-- 구체적으로 어떤 시험이 요구되는지는 확인 전이므로 질문 형태로만 남긴다.
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-002', 20,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"DIRECT_BODY_CONTACT","operator":"EQ","value":true},
     {"type":"attr","attribute":"HAS_TEMPERATURE_CONTROLLER","operator":"EQ","value":false}
   ]}',
 '[
     {"type":"flagExpertReview","question":"온도조절기(과열 방지 장치)가 없습니다. 장시간 사용 시 과열·화상 위험과 관련해 어떤 시험이 필요한지 확인이 필요합니다.","reason":"NO_EVIDENCE"}
   ]',
 '온도조절기 미탑재 시 과열 위험 확인 필요');

-- ── R-EH-010: 표시·라벨링 (전기용품 공통) ────────────────────────────────────
-- 정격전압·소비전력 표시는 전기용품 공통 사항이라 확인 전에도 안내할 수 있다.
-- 발열 제품 고유의 표시사항(최고온도·사용시간 경고 등)은 [TODO-확인후] 추가한다.
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-010', 30,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"USES_ELECTRICITY","operator":"EQ","value":true},
     {"type":"attr","attribute":"RATED_VOLTAGE","operator":"GT","value":50}
   ]}',
 '[
     {"type":"addLabelingCheck","label":"정격전압·소비전력 표시"},
     {"type":"addLabelingCheck","label":"제조자(수입자)·제조국 표시"},
     {"type":"requireDocument","documentCode":"SAFETY_LABEL_SAMPLE","requirement":"RECOMMENDED"}
   ]',
 '전기용품 공통 표시사항. 발열 제품 고유 표시는 공식 확인 후 추가');

-- ── R-EH-020: 어린이용 → 어린이제품 안전인증 후보 ────────────────────────────
-- 소형가전 룰셋(R-SA-010)과 동일한 근거를 쓴다.
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-020', 10,
 '{"type":"attr","attribute":"TARGET_USER","operator":"EQ","value":"CHILD"}',
 '[
     {"type":"addCandidate","schemeCode":"KC_CHILD_SAFETY_CERT","certificationType":"SAFETY_CERT"}
   ]',
 '어린이용 제품은 어린이제품 안전인증 대상 후보');

-- ── R-EH-003: 의료적 효능 표방 → 의료기기 규제 확인 ──────────────────────────
-- 혈액순환·통증 완화 등을 표방하면 전기용품이 아니라 의료기기법 적용 대상이 될 수 있다.
-- 구체적 분류는 단정하지 않고, 규제 영역이 달라질 수 있음을 확인하도록 안내한다(불변식 6).
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-003', 5,
 '{"type":"attr","attribute":"MEDICAL_USE_CLAIM","operator":"EQ","value":true}',
 '[
     {"type":"flagExpertReview","question":"혈액순환·통증 완화 등 의료적 효능을 표시·광고하면 전기용품이 아니라 의료기기로 분류될 수 있어 인증 경로가 완전히 달라집니다. 표방 문구가 의료적 효능에 해당하는지 확인해 주세요.","reason":"NO_EVIDENCE"}
   ]',
 '의료적 효능 표방 시 의료기기 규제 가능성 — 규제 영역 자체가 달라진다');

-- ── R-EH-004: 과열 보호 장치 없음 → 화상·과열 위험 확인 ──────────────────────
-- 온도조절기(단계 조절)와 별개로, 이상 과열 시 전원을 끊는 보호 장치의 유무는 화상 위험과 직결된다.
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-004', 20,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"DIRECT_BODY_CONTACT","operator":"EQ","value":true},
     {"type":"attr","attribute":"OVERHEAT_PROTECTION","operator":"EQ","value":false}
   ]}',
 '[
     {"type":"flagExpertReview","question":"과열 방지(온도 제한) 장치가 없습니다. 신체에 닿는 발열 제품은 이상 과열 시 화상 위험이 커, 어떤 안전 장치·시험이 요구되는지 확인이 필요합니다.","reason":"NO_EVIDENCE"}
   ]',
 '과열 방지 장치 미탑재 시 화상 위험 확인 필요');

-- ── R-EH-005: 세탁 가능한데 전기부 분리 불가 → 감전 위험 확인 ────────────────
-- 물세탁이 가능한데 열선·컨트롤러를 분리할 수 없으면 세탁 후 감전·절연 손상 위험이 있다.
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-005', 25,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"WASHABLE","operator":"EQ","value":true},
     {"type":"attr","attribute":"SEPARABLE_ELECTRIC_PARTS","operator":"EQ","value":false}
   ]}',
 '[
     {"type":"flagExpertReview","question":"물세탁이 가능하지만 열선·컨트롤러 등 전기부를 분리할 수 없습니다. 세탁 후 감전·절연 손상 위험과 관련해 방수·절연 시험 요건을 확인해 주세요.","reason":"NO_EVIDENCE"}
   ]',
 '세탁 가능 + 전기부 분리 불가 시 감전·절연 위험 확인');

-- ── R-EH-011: 커버 분리형 → 세탁·분리 방법 표시 ──────────────────────────────
-- 커버를 분리·세탁할 수 있으면 그 방법과 전기부 취급 주의사항을 표시해야 한다(표시사항 공통).
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-011', 30,
 '{"type":"attr","attribute":"REMOVABLE_COVER","operator":"EQ","value":true}',
 '[
     {"type":"addLabelingCheck","label":"커버 분리·세탁 방법 및 전기부 취급 주의사항 표시"}
   ]',
 '커버 분리형 제품의 세탁·분리 방법 표시');

-- ── R-EH-030: 별도 어댑터 미인증 → 어댑터 인증 확인 ──────────────────────────
-- 별도 전원 어댑터를 쓰는데 어댑터 자체가 인증받지 않았다면, 어댑터도 인증 범위에 포함되는지
-- 확인이 필요하다. 인증받은 어댑터(adapterCertified=true)는 범위가 달라질 수 있어 별도 판단한다.
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-030', 40,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"HAS_SEPARATE_ADAPTER","operator":"EQ","value":true},
     {"type":"attr","attribute":"ADAPTER_CERTIFIED","operator":"EQ","value":false}
   ]}',
 '[
     {"type":"flagExpertReview","question":"별도 전원 어댑터를 쓰지만 어댑터 자체 인증이 확인되지 않았습니다. 어댑터가 인증 대상·범위에 포함되는지 확인해 주세요.","reason":"NO_EVIDENCE"}
   ]',
 '미인증 별도 어댑터 사용 시 어댑터 인증 범위 확인 필요');

-- ── R-EH-090: 표면온도 미상 → 판단 불가 ──────────────────────────────────────
-- 정격전압 미상(R-SA-090)과 같은 규약이다. 모른다고 진단을 막지 않고 전문가 확인으로 보낸다.
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-090', 90,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"DIRECT_BODY_CONTACT","operator":"EQ","value":true},
     {"type":"attr","attribute":"MAX_SURFACE_TEMPERATURE","operator":"EQ","value":null}
   ]}',
 '[
     {"type":"flagExpertReview","question":"최고 표면온도를 모르면 화상 위험 기준 충족 여부를 판단할 수 없습니다. 시험기관에서 표면온도를 측정해 확인해 주세요.","reason":"AMBIGUOUS_CONDITION"}
   ]',
 '표면온도 미상 시 판단 불가 → 전문가 확인');

-- ── R-EH-091: 전기 사용인데 정격전압 미상 → 판단 불가 ────────────────────────
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000002', 'R-EH-091', 91,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"USES_ELECTRICITY","operator":"EQ","value":true},
     {"type":"attr","attribute":"RATED_VOLTAGE","operator":"EQ","value":null}
   ]}',
 '[
     {"type":"flagExpertReview","question":"정격전압 정보가 없어 적용 제도를 판단할 수 없습니다. 어댑터·전원부에 표시된 정격전압을 확인해 주세요.","reason":"AMBIGUOUS_CONDITION"}
   ]',
 '정격전압 미상 시 판단 불가 → 전문가 확인');
