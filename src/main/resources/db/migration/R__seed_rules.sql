-- 소형가전 데모 룰셋 v1 시드. 반복 실행 마이그레이션.
-- JSON 형식은 RuleJsonCodec이 파싱하는 형식과 정확히 일치해야 한다(RuleJsonCodecTest가 검증).
-- 실제 KC 제도를 단순화한 데모 규칙이며, 드라이기류 대표 시나리오를 검증한다(기획서).

-- 멱등성: 기존 시드를 지우고 다시 넣는다. rule은 FK CASCADE로 함께 삭제된다.
DELETE FROM rule_set WHERE product_group = 'SMALL_APPLIANCE' AND version = 1;

INSERT INTO rule_set (id, version, product_group, active, activated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 1, 'SMALL_APPLIANCE', true, now());

-- R-SA-001: 전기 사용 + 정격전압 > 50V → 안전확인 후보 + 필수/권장 서류
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000001', 'R-SA-001', 10,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"USES_ELECTRICITY","operator":"EQ","value":true},
     {"type":"attr","attribute":"RATED_VOLTAGE","operator":"GT","value":50}
   ]}',
 '[
     {"type":"addCandidate","schemeCode":"KC_SAFETY_CONFIRM_ELECTRIC","certificationType":"SAFETY_CONFIRM"},
     {"type":"requireDocument","documentCode":"BIZ_LICENSE","requirement":"REQUIRED"},
     {"type":"requireDocument","documentCode":"TEST_REPORT","requirement":"REQUIRED"},
     {"type":"requireDocument","documentCode":"CIRCUIT_DIAGRAM","requirement":"RECOMMENDED"},
     {"type":"requireDocument","documentCode":"PARTS_LIST","requirement":"RECOMMENDED"}
   ]',
 '전기 사용 + 정격전압 50V 초과 소형가전은 안전확인 대상 후보');

-- R-SA-002: 전기 사용 + 정격전압 > 50V → 표시·라벨링 확인 + 안전표시 견본
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000001', 'R-SA-002', 20,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"USES_ELECTRICITY","operator":"EQ","value":true},
     {"type":"attr","attribute":"RATED_VOLTAGE","operator":"GT","value":50}
   ]}',
 '[
     {"type":"addLabelingCheck","label":"KC 마크 및 안전확인 표시"},
     {"type":"addLabelingCheck","label":"정격전압·소비전력 표시"},
     {"type":"requireDocument","documentCode":"SAFETY_LABEL_SAMPLE","requirement":"REQUIRED"}
   ]',
 '전기용품 표시·라벨링 확인 항목');

-- R-SA-010: 어린이용 → 어린이제품 안전인증 후보
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000001', 'R-SA-010', 10,
 '{"type":"attr","attribute":"TARGET_USER","operator":"EQ","value":"CHILD"}',
 '[
     {"type":"addCandidate","schemeCode":"KC_CHILD_SAFETY_CERT","certificationType":"SAFETY_CERT"}
   ]',
 '어린이용 제품은 어린이제품 안전인증 대상 후보');

-- R-SA-090: 전기 사용인데 정격전압 정보 없음 → 판단 불가, 전문가 확인
INSERT INTO rule (rule_set_id, rule_code, priority, condition, effects, description) VALUES
('00000000-0000-0000-0000-000000000001', 'R-SA-090', 90,
 '{"type":"allOf","conditions":[
     {"type":"attr","attribute":"USES_ELECTRICITY","operator":"EQ","value":true},
     {"type":"attr","attribute":"RATED_VOLTAGE","operator":"EQ","value":null},
     {"type":"not","condition":{"type":"attr","attribute":"HAS_BATTERY","operator":"EQ","value":true}}
   ]}',
 '[
     {"type":"flagExpertReview","question":"정격전압 정보가 없어 안전확인 대상 여부를 판단할 수 없습니다. 정격전압을 확인해 주세요.","reason":"AMBIGUOUS_CONDITION"}
   ]',
 '정격전압 미상 시 판단 불가 → 전문가 확인');
