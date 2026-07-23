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
