-- 준비도 점수 가중치 기준표 시드. 반복 실행 마이그레이션(체크섬 변경 시 재적용).
-- 정부·공인 시험·인증기관이 공개한 반복 요구 항목을 체크리스트화하고, 필수 서류에 높은 가중치를 둔다.

DELETE FROM document_weight;

INSERT INTO document_weight (document_code, display_name, requirement, weight, note) VALUES
    ('BIZ_LICENSE',         '사업자등록증',      'REQUIRED',    3, '필수 제출 서류'),
    ('TEST_REPORT',         '시험성적서',        'REQUIRED',    3, '지정 시험기관 발행'),
    ('SAFETY_LABEL_SAMPLE', '안전표시 견본',     'REQUIRED',    3, 'KC 마크·정격 표시'),
    ('CIRCUIT_DIAGRAM',     '전기회로도',        'RECOMMENDED', 1, '상담 시 검토 자료'),
    ('PARTS_LIST',          '부품명세표',        'RECOMMENDED', 1, '상담 시 검토 자료');
