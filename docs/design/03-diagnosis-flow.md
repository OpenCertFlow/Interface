# 03. 진단 흐름

## 3.1 전체 시퀀스

```mermaid
sequenceDiagram
    autonumber
    actor U as 사용자 (소공인)
    participant A as Android 앱
    participant C as DiagnosisController
    participant S as DiagnoseProductService<br/>@UseCase
    participant R as RuleEvaluator<br/>(도메인 서비스)
    participant DB as PostgreSQL
    participant W as AI 워커
    participant L as LLM API

    U->>A: 제품 정보 입력 + 동의
    A->>C: POST /api/v1/diagnoses
    C->>C: 요청 DTO 검증 (Bean Validation)
    C->>S: diagnose(DiagnoseCommand)

    Note over S,DB: ── 1단계. 룰 평가 (결정론) ──
    S->>DB: LoadRuleSetPort.loadActive(제품군)
    DB-->>S: RuleSet(version=3)
    S->>R: evaluate(ProductProfile, RuleSet)
    R-->>S: 인증 후보 · 필수 서류 · 확인 항목

    Note over S: ── 2단계. 점수 산정 (결정론) ──
    S->>S: ScoreCalculator.calculate(요구서류, 보유서류)
    Note right of S: 이 시점에 진단 판정은 확정.<br/>이후 단계는 판정을 바꾸지 못한다.

    Note over S,L: ── 3단계. 근거 검색 (확률론, 폴백 가능) ──
    S->>W: SearchEvidencePort.search(제도명, 후보)
    W->>W: Qdrant 벡터 검색 + 리랭킹
    W-->>S: Evidence[] (원문 링크 포함)

    Note over S,L: ── 4단계. 문장화 (확률론, 폴백 가능) ──
    S->>W: NarrateReportPort.narrate(결과, 근거)
    W->>L: 프롬프트 (규칙 결과 + 근거 + 면책)
    L-->>W: 리포트 문장
    W-->>S: Narration

    Note over S,DB: ── 5단계. 저장 ──
    S->>DB: SaveDiagnosisPort.save(Diagnosis)
    DB-->>S: DiagnosisId

    S-->>C: DiagnosisReport
    C-->>A: 201 Created + 리포트 JSON
    A-->>U: 준비도 점수 · 누락 서류 · 근거 · 컨설팅 버튼
```

1~2단계와 3~4단계 사이의 주석이 이 시스템의 핵심입니다. **2단계가 끝나면 진단 결과는 이미 확정**되어 있고, 3~4단계는 그 결과에 근거와 설명을 덧붙일 뿐입니다.

## 3.2 룰 평가

```mermaid
flowchart TD
    START(["ProductProfile 입력"]) --> NORM["입력값 표준화<br/>전압 단위 · 제품군 코드 · 서류 코드"]
    NORM --> LOAD["활성 RuleSet 로드<br/>(version 고정)"]
    LOAD --> LOOP{"각 Rule에 대해<br/>조건 평가"}

    LOOP -->|"조건 일치"| MATCH["Rule 효과 누적<br/>· 인증 후보 추가<br/>· 필수 서류 추가<br/>· 표시·라벨링 항목 추가<br/>· 전문가 확인 항목 추가"]
    LOOP -->|"불일치"| SKIP["건너뜀"]
    MATCH --> NEXT
    SKIP --> NEXT
    NEXT{"남은 Rule?"} -->|예| LOOP
    NEXT -->|아니오| EXC["예외 조건 적용<br/>(우선순위 높은 Rule이 덮어씀)"]

    EXC --> DECIDE{"인증 후보가<br/>하나라도 있는가?"}
    DECIDE -->|"예"| OK["RuleEvaluationResult<br/>+ 매칭된 ruleId 목록"]
    DECIDE -->|"아니오"| UNKNOWN["'해당 없음 또는 판단 불가'<br/>→ 전체를 전문가 확인 필요로 분류"]

    OK --> OUT(["결과 + ruleSetVersion"])
    UNKNOWN --> OUT

    style UNKNOWN fill:#fff3e0,stroke:#ef6c00
```

`UNKNOWN` 경로가 중요합니다. 룰이 아무것도 못 잡았을 때 LLM에게 "그럼 네가 판단해봐"라고 넘기는 순간 서비스의 신뢰성 전제가 무너집니다. 판단하지 못했다는 사실을 그대로 사용자에게 전달하고 전문가 상담으로 연결하는 것이 정답입니다.

**매칭된 `ruleId` 목록을 결과에 함께 저장**합니다. "왜 이 인증이 후보로 나왔는가"를 사후에 추적할 수 있어야 하고, 이것이 검증 단계의 "규칙 일치 여부 확인" 방법이 됩니다.

## 3.3 준비도 점수 산정

점수는 합격 가능성이 아니라 **공식 요구자료 대비 현재 준비 수준**입니다. 계산식은 단순해야 하고, 사용자에게 그대로 보여줄 수 있어야 합니다.

```mermaid
flowchart LR
    subgraph input["입력"]
        REQ["요구 서류 목록<br/>(룰 평가 결과)"]
        HELD["보유 서류 체크<br/>(사용자 입력)"]
    end

    subgraph calc["산정"]
        W["각 서류에 가중치 부여<br/>필수 = 3 · 권장 = 1"]
        NUM["분자 = Σ(보유한 서류의 가중치)"]
        DEN["분모 = Σ(요구 서류의 가중치)"]
        DIV["점수 = round(분자 / 분모 × 100)"]
    end

    subgraph output["출력"]
        SCORE["준비도 점수 (%)"]
        MISS["누락 서류<br/>가중치 내림차순 = 보완 우선순위"]
    end

    REQ --> W
    HELD --> NUM
    W --> NUM
    W --> DEN
    NUM --> DIV
    DEN --> DIV
    DIV --> SCORE
    W --> MISS
```

가중치를 코드에 하드코딩하지 않고 `document_weight` 테이블에 두는 이유는, 점수 기준표가 산출물 목록에 포함되어 있고 심사에서 근거를 요구받기 때문입니다. 가중치를 바꿔도 코드 배포가 필요 없어야 합니다.

**보완 우선순위 = 누락 서류를 가중치 내림차순으로 정렬한 것**입니다. 별도 알고리즘이 아닙니다. 점수를 가장 많이 올리는 순서가 곧 우선순위입니다.

## 3.4 장애 폴백 정책

```mermaid
flowchart TD
    REQ(["진단 요청"]) --> RULE{"룰셋 로드<br/>성공?"}
    RULE -->|"실패"| E503["503 Service Unavailable<br/>진단 불가 — 재시도 안내"]
    RULE -->|"성공"| EVAL["룰 평가 · 점수 산정<br/>(판정 확정)"]

    EVAL --> RAG{"RAG 검색<br/>2초 내 성공?"}
    RAG -->|"타임아웃 / 실패"| RAGF["근거 없이 진행<br/>degraded.evidence = true<br/>리포트에 '근거 조회 실패' 배너"]
    RAG -->|"성공"| EV["Evidence 첨부"]

    RAGF --> NARR
    EV --> NARR{"LLM 문장화<br/>5초 내 성공?"}
    NARR -->|"타임아웃 / 실패"| NARRF["템플릿 문장으로 대체<br/>degraded.narration = true"]
    NARR -->|"성공"| NA["LLM 문장 사용"]

    NARRF --> SAVE
    NA --> SAVE["진단 결과 저장<br/>(degraded 플래그 포함)"]
    SAVE --> OK200["200 OK + 리포트"]

    style E503 fill:#ffebee,stroke:#c62828
    style RAGF fill:#fff3e0,stroke:#ef6c00
    style NARRF fill:#fff3e0,stroke:#ef6c00
    style OK200 fill:#e8f5e9,stroke:#2e7d32
```

| 실패 지점 | 결과 | 사용자에게 보이는 것 |
| --- | --- | --- |
| 룰셋 로드 | **진단 실패 (503)** | "일시적으로 진단할 수 없습니다" |
| RAG 검색 | 진단 성공, 근거 없음 | 점수·서류는 정상, "공식 근거를 불러오지 못했습니다" 배너 |
| LLM 문장화 | 진단 성공, 템플릿 문장 | 점수·서류·근거는 정상, 설명 문장만 정형화됨 |
| 진단 저장 | **진단 실패 (500)** | "저장에 실패했습니다" |

`degraded` 플래그를 응답과 DB에 모두 남깁니다. 시연 중 네트워크가 흔들려도 데모는 계속 돌아가야 하고, 사후에 "그때 왜 근거가 비어 있었는가"를 답할 수 있어야 합니다.

## 3.5 상태 전이

```mermaid
stateDiagram-v2
    [*] --> REQUESTED: 진단 요청 접수
    REQUESTED --> RULE_EVALUATED: 룰 평가 · 점수 산정 완료
    REQUESTED --> FAILED: 룰셋 로드 실패

    RULE_EVALUATED --> COMPLETED: 근거 · 문장화 성공
    RULE_EVALUATED --> COMPLETED_DEGRADED: 근거 또는 문장화 실패
    RULE_EVALUATED --> FAILED: 저장 실패

    COMPLETED --> CONSULTING_REQUESTED: 컨설팅 연결 요청
    COMPLETED_DEGRADED --> CONSULTING_REQUESTED

    COMPLETED --> [*]
    COMPLETED_DEGRADED --> [*]
    FAILED --> [*]
    CONSULTING_REQUESTED --> [*]

    note right of COMPLETED_DEGRADED
        판정과 점수는 COMPLETED와 동일하게 유효.
        근거 또는 설명 문장만 결여된 상태.
    end note
```

## 3.6 API 개요

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/diagnoses` | 진단 실행 — 제품 정보 + 동의 |
| `GET` | `/api/v1/diagnoses/{id}` | 진단 리포트 조회 |
| `GET` | `/api/v1/diagnoses/{id}/evidences` | 근거 문단 목록 (원문 링크) |
| `POST` | `/api/v1/consulting-leads` | 컨설팅 연결 요청 |
| `GET` | `/api/v1/product-groups` | 제품군 · 입력 스키마 메타데이터 |
| `GET` | `/actuator/health` | 헬스체크 |

모든 응답은 `ApiResponse<T>` 봉투로 감쌉니다. Android 팀이 성공/실패 분기를 한 곳에서 처리할 수 있도록 하기 위함이고, `traceId`를 항상 실어 보내 장애 시 로그 추적이 가능하게 합니다.
