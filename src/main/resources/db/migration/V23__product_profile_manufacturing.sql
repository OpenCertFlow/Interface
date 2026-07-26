-- 제품 입력 항목 보강 (F-APP-006/008): 제조 형태·변경모델 여부
--
-- manufacturing_type: 자체/수입/OEM/ODM/모름. 인증 책임 주체와 필요 서류가 달라진다.
-- is_modified_model: 기존 인증 모델을 변경한 제품인지. 변경 시 기존 인증 범위 확인이 필요하다.
--
-- 둘 다 nullable이다. 이전 진단 행은 값이 없어 null로 남고, 매퍼가 조회 시 '모름'·false로 복원한다.

ALTER TABLE product_profile ADD COLUMN manufacturing_type varchar(20);
ALTER TABLE product_profile ADD COLUMN is_modified_model boolean;
