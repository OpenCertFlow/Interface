-- 준비도 점수 가중치 기준표 시드. 반복 실행 마이그레이션(체크섬 변경 시 재적용).
-- 정부·공인 시험·인증기관이 공개한 반복 요구 항목을 체크리스트화하고, 필수 서류에 높은 가중치를 둔다.
--
-- ON CONFLICT DO NOTHING으로 기본값을 채우기만 하고 기존 행은 건드리지 않는다. 관리자 API
-- (F-WADM-011)로 가중치를 조정한 뒤 이 시드가 다시 실행돼도 그 편집이 지워지지 않게 하기 위함이다.
-- 즉 시드는 "없으면 기본값을 넣는" 역할이고, 운영 중 값은 관리 API가 권위를 갖는다.

INSERT INTO document_weight (document_code, display_name, requirement, weight, note) VALUES
    ('BIZ_LICENSE',         '사업자등록증',      'REQUIRED',    3, '필수 제출 서류'),
    ('TEST_REPORT',         '시험성적서',        'REQUIRED',    3, '지정 시험기관 발행'),
    ('SAFETY_LABEL_SAMPLE', '안전표시 견본',     'REQUIRED',    3, 'KC 마크·정격 표시'),
    ('CIRCUIT_DIAGRAM',     '전기회로도',        'RECOMMENDED', 1, '상담 시 검토 자료'),
    ('PARTS_LIST',          '부품명세표',        'RECOMMENDED', 1, '상담 시 검토 자료')
ON CONFLICT (document_code) DO NOTHING;
