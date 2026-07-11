# AI 워커 API 계약

백엔드 ↔ AI/RAG 워커(Python·FastAPI) 사이의 HTTP 계약이다. [ADR-0004](../design/adr/0004-separate-ai-worker.md)에 따라 이 계약을 먼저 고정하고 양측이 독립적으로 개발한다. 변경은 양측 합의로만 한다.

- 백엔드 측 구현: `RagSearchAdapter`, `LlmNarrationAdapter` (`!local` 프로파일)
- 로컬 데모에서는 `StubEvidenceSearchAdapter`·`StubNarrationAdapter`(`local` 프로파일)가 대신한다
- 모든 요청에 `X-Trace-Id` 헤더가 전파된다. 워커는 이 값을 로그에 남긴다(분산 추적의 최소 형태).

## 응답 시간 예산

| 엔드포인트 | 예산 | 초과 시 |
| --- | --- | --- |
| `POST /search` | 2초 | 백엔드가 근거 없이 진행 (`degraded.evidence`) |
| `POST /narrate` | 5초 | 백엔드가 템플릿 문장으로 폴백 (`degraded.narration`) |

타임아웃과 폴백은 백엔드의 정책이다. 워커는 자기 시간 안에 최선의 결과를 주면 되고, 실패 시 4xx/5xx 또는 지연으로 알리면 된다. **워커가 빈 결과를 지어내지 않는다** — 못 찾으면 빈 배열, 오류면 에러 상태.

---

## POST /search

룰이 식별한 후보로 범위를 좁혀 공식 문서 근거를 검색한다.

### 요청

```json
{
  "productGroup": "SMALL_APPLIANCE",
  "schemeCodes": ["KC_SAFETY_CONFIRM_ELECTRIC"],
  "certificationTypes": ["SAFETY_CONFIRM"],
  "sections": ["DOCUMENTS", "LABELING"]
}
```

### 응답 (200)

```json
{
  "evidences": [
    {
      "sourceDocumentId": "doc-electric-safety-01",
      "sectionType": "DOCUMENTS",
      "snippet": "안전확인대상 전기용품은 지정 시험기관의 시험을 거쳐...",
      "sourceUrl": "https://www.safetykorea.kr/...",
      "relevance": 0.83
    }
  ]
}
```

- `sourceUrl`은 **필수**다. 출처 없는 근거는 백엔드가 거부한다(불변식 6). 임계 유사도(권장 0.65) 미달 근거는 워커가 걸러서 보내지 않는다.
- 근거가 없으면 `{"evidences": []}`. 빈 배열은 정상이며 저하가 아니다.

---

## POST /narrate

확정된 판정·점수·근거를 사용자가 이해하기 쉬운 문장으로 옮긴다. **LLM은 판정을 바꾸지 못한다** — 입력값을 문장으로 정리만 한다([ADR-0003](../design/adr/0003-rule-engine-over-llm.md)).

### 요청

```json
{
  "productName": "가정용 헤어드라이어",
  "productGroup": "SMALL_APPLIANCE",
  "score": { "applicable": true, "percentage": 43 },
  "candidates": [
    { "schemeCode": "KC_SAFETY_CONFIRM_ELECTRIC", "certificationType": "SAFETY_CONFIRM" }
  ],
  "requiredDocuments": [
    { "documentCode": "BIZ_LICENSE", "requirement": "REQUIRED", "held": false }
  ],
  "missingDocuments": ["BIZ_LICENSE", "SAFETY_LABEL_SAMPLE"],
  "expertReviewItems": [
    { "question": "정격전압이 정확히 몇 V인가요?", "reason": "AMBIGUOUS_CONDITION" }
  ],
  "evidences": [
    { "sectionType": "DOCUMENTS", "snippet": "...", "sourceUrl": "https://..." }
  ]
}
```

### 응답 (200)

```json
{
  "summary": "220V 드라이기는 안전확인 대상으로 보입니다. 현재 준비도는 43%입니다.",
  "nextActions": ["KTC 또는 KTR 같은 시험기관에서 시험성적서를 받으세요."],
  "preConsultQuestions": ["정격전압이 정확히 몇 V인가요?"],
  "disclaimer": "본 결과는 사전 점검 지표이며 인증 합격을 보장하지 않습니다.",
  "modelId": "claude-opus-4-8"
}
```

프롬프트는 입력값·규칙 결과·검색 근거·면책 문구를 템플릿화해 구성하고, 근거가 부족한 내용은 단정하지 않고 `expertReviewItems`로 넘긴다. 근거(`evidences`)가 비어 있으면 워커는 그 사실을 알고 단정을 피한다.

---

## 오류

| 상태 | 의미 | 백엔드 처리 |
| --- | --- | --- |
| `4xx` | 잘못된 요청 (계약 위반) | 폴백 후 로그 — 개발 중 계약 불일치 신호 |
| `5xx` | 워커 내부 오류 (LLM/벡터DB 장애) | 폴백 |
| 타임아웃 | 예산 초과 | 폴백 |

백엔드는 모든 실패를 `ExternalSystemException`으로 감싸 폴백을 결정한다. 진단은 어떤 경우에도 완료된다 — AI 워커 장애가 진단 실패로 번지지 않는다.
