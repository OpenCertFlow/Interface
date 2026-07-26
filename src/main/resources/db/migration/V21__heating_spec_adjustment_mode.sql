-- 전기방석 입력 항목 보강 (결정문 §5.2): 온도 조절 방식·온도제한장치
--
-- adjustment_mode: 온도 조절 방식(STEP/CONTINUOUS/OTHER). 조절기가 있을 때만 값이 있고, 단계 수
--   (adjustment_steps)은 방식이 STEP일 때만 존재한다. 짝 규칙은 도메인 HeatingSpec이 강제한다.
-- temperature_limit_device: 표면온도 상한을 제한하는 장치(과열 차단 overheat_protection과 별개).
--
-- 둘 다 nullable이다. 기존 진단 행(전기방석 이전·소형가전)은 발열 사양이 없어 null로 남는다.

ALTER TABLE product_profile ADD COLUMN adjustment_mode varchar(20);
ALTER TABLE product_profile ADD COLUMN temperature_limit_device boolean;
