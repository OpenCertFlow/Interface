-- 리포트 문구 시드. ON CONFLICT DO NOTHING으로 기본값만 채우고 관리자 편집은 보존한다.

INSERT INTO report_phrase (phrase_key, text, description) VALUES
    ('REPORT_DISCLAIMER',
     '본 결과는 인증 합격·불합격 판정이 아니라 준비 상태를 확인하기 위한 사전 점검 지표입니다. 실제 인증 가능 여부는 지정 시험·인증기관의 판단을 따릅니다.',
     '리포트 하단 면책 문구'),
    ('REPORT_INTRO',
     '입력하신 제품 정보를 바탕으로 인증 준비 상태를 점검한 결과입니다.',
     '리포트 상단 안내'),
    ('CONSULT_GUIDE',
     '보완이 필요한 항목은 전문가 상담을 통해 구체적으로 확인하실 수 있습니다.',
     '상담 유도 안내')
ON CONFLICT (phrase_key) DO NOTHING;
