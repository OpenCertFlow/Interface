# 01. 시스템 아키텍처

## 1.1 전체 구성

```mermaid
flowchart TB
    subgraph client["클라이언트"]
        AOS["Android 앱<br/>Kotlin · Jetpack Compose<br/>제품 입력 / 리포트 / 컨설팅"]
    end

    subgraph backend["백엔드 — Spring Boot WebFlux (Java 17)"]
        API["REST API<br/>진단 요청 · 리포트 조회 · 컨설팅 접수"]
        RULE["Rule Engine<br/>인증 후보 1차 식별 · 서류 체크"]
        SCORE["Scoring<br/>가중치 기반 준비도 점수"]
        ORCH["Diagnosis Orchestrator<br/>규칙 → 근거 → 점수 → 문장화"]
    end

    subgraph worker["AI/RAG 워커 — Python · FastAPI"]
        RAG["RAG 검색<br/>근거 문단 retrieval + rerank"]
        NARR["리포트 문장화<br/>LLM 프롬프트 오케스트레이션"]
        INGEST["문서 수집·정제 파이프라인<br/>chunking · embedding"]
    end

    subgraph data["데이터 저장소"]
        PG[("PostgreSQL<br/>룰 · 문서 메타데이터<br/>진단 결과 · 컨설팅 리드")]
        QD[("Qdrant<br/>문서 청크 임베딩")]
    end

    subgraph ext["외부"]
        LLM["상용 LLM API"]
        SRC["공식 인증자료<br/>Safety Korea · KTC · KTR"]
    end

    AOS -- "HTTPS / JSON" --> API
    API --> ORCH
    ORCH --> RULE
    ORCH --> SCORE
    ORCH -- "근거 검색 (HTTP)" --> RAG
    ORCH -- "문장화 (HTTP)" --> NARR
    RULE -- "룰셋 로드" --> PG
    ORCH -- "진단 결과 저장" --> PG
    RAG --> QD
    RAG -- "청크 메타데이터" --> PG
    NARR --> LLM
    INGEST -- "수집" --> SRC
    INGEST --> QD
    INGEST --> PG

    classDef fallback stroke-dasharray: 5 5
    class RAG,NARR fallback
```

점선 테두리(`RAG`, `NARR`)는 **실패해도 진단이 완료되는** 컴포넌트입니다. 기획서의 "API 호출 실패, RAG 검색 실패, 외부 LLM 응답 지연 상황에 대비해 기본 규칙 기반 결과와 오류 안내 메시지를 우선 제공한다"를 아키텍처 제약으로 못 박은 것입니다. 자세한 폴백 규칙은 [03-diagnosis-flow.md](03-diagnosis-flow.md#34-장애-폴백-정책)에 있습니다.

## 1.2 컴포넌트 책임 경계

Spring Boot 백엔드와 Python 워커를 나누는 기준은 "언어 취향"이 아니라 **결정론이 필요한가**입니다.

```mermaid
flowchart LR
    subgraph det["결정론 영역 — Java / 백엔드"]
        direction TB
        D1["입력값 표준화"]
        D2["Rule Engine 조건 평가"]
        D3["서류 체크리스트 대조"]
        D4["가중치 점수 산정"]
        D1 --> D2 --> D3 --> D4
    end

    subgraph prob["확률론 영역 — Python / AI 워커"]
        direction TB
        P1["벡터 검색 (top-k)"]
        P2["리랭킹"]
        P3["LLM 문장 생성"]
        P1 --> P2 --> P3
    end

    det -- "확정된 결과를 넘김" --> prob
    prob -- "근거 · 설명만 되돌림<br/>(판정은 바꾸지 못함)" --> det
```

**단방향 제약**: AI 워커의 응답은 진단 결과의 *판정*을 변경할 수 없습니다. 근거(`Evidence`)를 붙이거나 설명(`Narration`)을 채울 뿐입니다. 이 제약이 "LLM이 근거 없이 인증명이나 판단을 생성하는 위험"을 구조적으로 차단합니다.

## 1.3 요청 경로별 성격

| 경로 | 특성 | 처리 방식 |
| --- | --- | --- |
| Android → 백엔드 | 짧은 JSON, 다수 동시 요청 | WebFlux 논블로킹 |
| 백엔드 → PostgreSQL | 블로킹 JDBC (JPA) | 전용 `Scheduler`로 격리 ([ADR-0002](adr/0002-webflux-with-jpa.md)) |
| 백엔드 → AI 워커 | 수백 ms ~ 수 초 지연 | `WebClient` 논블로킹 + 타임아웃 + 폴백 |
| AI 워커 → LLM | 수 초 지연, 실패 가능 | 워커 내부에서 타임아웃, 백엔드는 폴백 보유 |

WebFlux를 택한 실익은 세 번째 줄에 있습니다. 진단 한 건이 RAG와 LLM을 기다리는 동안 스레드를 점유하지 않습니다.

## 1.4 배포 토폴로지

```mermaid
flowchart TB
    subgraph cloud["클라우드 VM"]
        subgraph compose["Docker Compose"]
            B["backend<br/>:8080"]
            W["ai-worker<br/>:8000"]
            P[("postgres:16<br/>:5432")]
            Q[("qdrant<br/>:6333")]
        end
    end

    subgraph ci["GitHub Actions"]
        T["test<br/>단위 · ArchUnit · Testcontainers"]
        BLD["build<br/>Gradle bootJar · Docker image"]
        DEP["deploy<br/>compose pull && up -d"]
        T --> BLD --> DEP
    end

    APK["Android APK<br/>내부 테스트 트랙"]

    DEP -.-> compose
    B --> P
    B --> W
    W --> Q
    W --> P
    APK -- "HTTPS" --> B
```

해커톤 규모에서 Kubernetes는 비용 대비 효익이 없습니다. 단일 VM + Compose로 시작하고, 컨테이너 경계만 정확히 나눠 두면 이후 이관은 어렵지 않습니다.

## 1.5 관련 결정 기록

- [ADR-0001: 헥사고날 아키텍처 채택](adr/0001-hexagonal-architecture.md)
- [ADR-0002: WebFlux + JPA 조합과 블로킹 격리](adr/0002-webflux-with-jpa.md)
- [ADR-0003: Rule Engine과 LLM의 책임 분리](adr/0003-rule-engine-over-llm.md)
- [ADR-0004: AI 워커를 별도 프로세스로 분리](adr/0004-separate-ai-worker.md)
