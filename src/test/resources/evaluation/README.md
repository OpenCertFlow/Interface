# 고정 평가데이터

기획서 2.6·5.3이 약속한 *"정상·모름·미확인·모순·근거부족·AI 장애·권한오류 사례를 고정
평가데이터로 검증한다"*를 실제 파일로 만든 것이다. 심사에서 "무엇으로 검증했는가"를 물으면
이 디렉토리를 연다.

**고정(fixed)이라는 말이 핵심이다.** 케이스를 마음대로 바꾸면 "지난번에 통과했다"가 의미를
잃는다. 케이스를 바꿀 때는 왜 바꾸는지를 커밋 메시지에 남긴다.

## 파일

| 파일 | 내용 |
| --- | --- |
| `cases.json` | 평가 케이스 정의. 입력과 기대 결과가 한 파일에 있다 |

## 구동

`FixedEvaluationDataTest`가 이 파일을 읽어 실제 API로 돌린다.

```bash
./gradlew test --tests '*FixedEvaluationDataTest'
```

## 케이스 구조

```json
{
  "id": "NORMAL-HAIR-DRYER",
  "category": "정상",
  "description": "모든 필수 서류를 보유한 모발건조기",
  "request": { ...POST /api/v1/diagnoses 요청 본문... },
  "expect": {
    "minCandidates": 1,
    "scoreApplicable": true,
    "minScore": 80,
    "unknownDocuments": 0,
    "evidenceDegraded": null
  }
}
```

`expect`의 필드는 모두 선택이다. 명시한 것만 검사한다 — 케이스마다 확인하고 싶은 것이 다르기
때문이고, 무관한 값까지 고정하면 관계없는 변경에 테스트가 깨진다.

| 필드 | 뜻 |
| --- | --- |
| `minCandidates` / `maxCandidates` | 인증 검토 후보 개수 하한·상한 |
| `scoreApplicable` | 준비도 산정 가능 여부 |
| `minScore` / `maxScore` | 준비도 점수 범위(%) |
| `unknownDocuments` | '확인 중'으로 분류된 서류 수 |
| `absentDocuments` | '누락'으로 분류된 서류 수 |
| `minExpertReviewItems` | 전문가 확인 항목 최소 개수 |
| `evidenceDegraded` | 근거 저하 플래그 기대값 (`null`이면 검사 안 함) |
| `httpStatus` | 기대 HTTP 상태. 명시하면 본문 검사를 하지 않는다 |

## 카테고리별로 무엇을 지키는가

| 카테고리 | 지키려는 성질 |
| --- | --- |
| 정상 | 기본 흐름이 후보·점수·체크리스트를 모두 낸다 |
| 모름 | '모름' 입력이 보유로도 미보유로도 해석되지 않는다 |
| 미확인 | 세부 수치나 등급을 모를 때 임의 판정 대신 전문가 확인으로 전환된다 |
| 모순 | 서로 어긋나는 입력에서 조용히 한쪽을 고르지 않는다 |
| AI 장애 | RAG·LLM이 없어도 규칙 결과와 점수는 유효하다 |
| 권한오류 | 없는 자원을 짐작해 열어 주지 않는다 |

## 케이스와 무관하게 항상 검사하는 것

**후보가 있는데 근거가 0건이면 `degraded.evidence`가 참이어야 한다.**
케이스가 무엇을 기대하든 매번 확인한다. 근거란이 조용히 비면 사용자는
"근거가 필요 없는 제품"으로 오해하기 때문이다.

후보가 없으면 검색할 것이 없으므로 저하가 아니다 — 그 경계도 함께 지킨다.

## 여기서 덮지 못하는 것

**근거부족(RAG가 0건을 반환하는 상황)은 이 데이터로 재현할 수 없다.**
`local` 프로파일의 근거 검색 스텁이 항상 1건을 돌려주기 때문이다.
해당 경로는 `DiagnoseProductServiceTest`의 단위 테스트가 페이크 포트로 덮는다.

**전기방석이 후보를 내지 않는 것은 버그가 아니라 현재 설계다.**
인증 등급이 공식 확인 전이라 룰셋이 등급을 단정하지 않고 `flagExpertReview`로 보낸다(이슈 #23).
등급이 확정되면 이 케이스의 `maxCandidates`를 `minCandidates`로 바꿔야 한다.
