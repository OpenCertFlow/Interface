# OpenCertFlow — 백엔드

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

**KC 제품안전 인증 사전진단 오픈소스 프레임워크.** 제품 속성과 공식 규정을 결합해 인증 적용
후보·준비도 점수·판단 근거·누락 서류·전문가 확인 항목을 산출합니다.

Spring Boot 3.5 (WebFlux) · Java 17 · PostgreSQL · 헥사고날 아키텍처

설계 문서는 [`docs/design/`](docs/design/README.md)에 있습니다. 코드를 만지기 전에
[02-hexagonal-architecture.md](docs/design/02-hexagonal-architecture.md)와
[ADR-0002](docs/design/adr/0002-webflux-with-jpa.md)를 읽어 주세요.

## 이 저장소에서 가장 중요한 디렉터리

인증 규칙은 코드가 아니라 **데이터**입니다. 서버를 몰라도 규칙은 읽고 고칠 수 있습니다.

| 경로 | 내용 |
|---|---|
| [`rules/`](rules/) | 제품군별 인증 룰셋 (YAML). **진실의 원천** — DB는 기동 시 이 파일로 채워지는 사본입니다 |
| [`weights/`](weights/) | 준비도 점수 가중치 기준표. "왜 72점인가"의 계산 근거 |
| [`schema/`](schema/) | 위 두 형식의 JSON Schema. IDE 자동완성과 CI 검증이 같은 파일을 씁니다 |
| [`openapi/`](openapi/) | API 스펙. SDK 3종이 여기서 생성되며, CI가 실제 API와 대조합니다 |

규칙을 고치려면 `rules/`의 YAML을 고쳐 PR을 보내면 됩니다. **DB도 서버도 필요 없습니다.**

```bash
./gradlew validateRules       # 구조·값 타입·의미 3중 검증
```

## 실행

```bash
docker compose up -d postgres redis qdrant     # 프로젝트 루트에서
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

`local` 프로파일은 AI 워커 스텁 + 실제 PostgreSQL을 쓴다(데모 구성). 진단 한 건 실행:

```bash
curl -s -X POST http://localhost:8080/api/v1/diagnoses \
  -H 'Content-Type: application/json' \
  -d '{
    "productName":"가정용 헤어드라이어","productGroup":"SMALL_APPLIANCE",
    "usesElectricity":true,"ratedVoltage":220,"powerConsumption":1200,"hasBattery":false,
    "targetUser":"GENERAL","salesChannel":"ONLINE",
    "materials":["PLASTIC","METAL"],"heldDocuments":["TEST_REPORT"]
  }'
# → 201, 안전확인 후보 · 준비도 점수 · 누락 서류 · 근거 · 컨설팅 연결 대상
```

- API 문서: http://localhost:8080/swagger-ui.html
- 헬스체크: http://localhost:8080/actuator/health

## CLI — 서버 없이 룰 검증하기

커뮤니티 기여자가 인프라를 세우지 않고도 규칙을 검증할 수 있어야 합니다. CLI는 스프링 컨텍스트를
띄우지 않고 파일과 순수 도메인 코드만 씁니다.

```bash
./gradlew validateRules                     # rules/ 전체 검증
./gradlew printRuntimeClasspath             # 직접 실행용 클래스패스

java -cp "$CP" io.opencertflow.cli.OpenCertFlowCli validate rules/
java -cp "$CP" io.opencertflow.cli.OpenCertFlowCli explain R-EH-005
```

검증은 세 겹입니다. 종료 코드는 `0` 정상 / `1` 지적 발견 / `2` 파일 읽기 실패입니다.

| 층 | 검사 주체 | 잡는 것 |
|---|---|---|
| 구조 | `schema/ruleset.schema.json` | 필드 누락, 잘못된 열거값 |
| 값 타입 | `RuleJsonCodec` | 속성이 기대하는 타입과 값의 불일치 |
| 의미 | `RuleConsistencyChecker` | 절대 발동하지 않는 룰, 중복 코드, 효과 없는 룰, 미사용 속성 |

`explain`은 룰을 사람의 언어로 풀어 줍니다 — 룰 코드만으로는 왜 그 결과가 나왔는지 설명할 수 없습니다.

```
R-EH-005  (ELECTRIC_HEATING_PAD v1, 우선순위 25)
  세탁 가능 + 전기부 분리 불가 시 감전·절연 위험 확인

  다음일 때 발동한다:
    모두 참:
      - WASHABLE = true
      - SEPARABLE_ELECTRIC_PARTS = false

  그러면:
    • 전문가 확인 필요 [NO_EVIDENCE]: 물세탁이 가능하지만 …
```

## SDK

`openapi/openapi.json`이 원본이고, 여기서 클라이언트가 생성됩니다.

```bash
./gradlew generateSdks         # TypeScript · Python · Kotlin → build/sdk/
./gradlew updateOpenApiSpec    # 스펙 갱신 (Docker 필요)
```

스펙이 코드보다 낡으면 SDK 사용자는 존재하지 않는 필드를 부르고 런타임에야 알게 됩니다. 사람이
기억하는 절차는 잊히므로 `OpenApiSpecTest`가 CI마다 실제 API와 대조해 드리프트를 실패로 만듭니다.

## 테스트와 품질 게이트

```bash
./gradlew test                 # 단위 · ArchUnit · BlockHound · Testcontainers
./gradlew check                # 위 + spotless + 룰 검증 + 스펙 드리프트
./gradlew pitest               # 도메인 뮤테이션 테스트
./gradlew jacocoTestReport     # 커버리지
```

`pitest`는 "테스트가 있다"가 아니라 **"테스트가 잡는다"**를 증명합니다. 룰 엔진처럼 틀리면 가장
위험한 순수 함수 영역에만 돌립니다.

## 지금 지켜야 하는 세 가지

**1. DB는 `BlockingBridge`로만 만진다.**

WebFlux 위에서 JPA를 씁니다. 영속성 어댑터를 이벤트 루프에서 직접 호출하면 Netty 워커 스레드가
JDBC I/O에 묶이고, **부하가 낮은 시연 환경에서는 증상이 보이지 않습니다.**

```java
// 이렇게
return blockingBridge.mono(() -> loadRuleSetPort.loadActive(productGroup))
        .switchIfEmpty(Mono.error(new BusinessException(SERVICE_UNAVAILABLE)));

// 이렇게 하면 안 된다 — 이벤트 루프가 막힌다
RuleSet ruleSet = loadRuleSetPort.loadActive(productGroup);
```

테스트에서 BlockHound가 위반을 잡습니다. `BlockHoundInstalledTest`가 그 방어선 자체를 검증합니다.

**2. 애플리케이션 서비스에 `@Transactional`을 붙이지 않는다.**

리액티브 체인을 가로지르지 못합니다. 트랜잭션은 영속성 어댑터 안에서 시작하고 끝납니다.
ArchUnit이 위반을 실패시킵니다.

**3. 도메인은 순수 자바다.**

`..domain..` 패키지에 `@Entity`, `@Component`, `Mono`, `Instant.now()`가 들어가면 `ArchitectureTest`가
빌드를 깹니다. 시각과 ID는 `TimeProvider`·`IdGenerator` 포트로 받습니다.

## 패키지 구조

```
io.opencertflow
├── common/          ✅ 공통 커널
│   ├── domain/      순수 자바. 오류 모델, 애그리거트 기반 클래스, 포트
│   ├── application/ @UseCase, BlockingBridge
│   ├── adapter/     웹 응답 봉투, 예외 핸들러, traceId, UUIDv7, JPA 감사
│   └── config/      구성 루트. 모든 계층을 알아도 되는 유일한 곳
├── diagnosis/
│   └── domain/      ✅ 룰 엔진 · 점수 산정 · Diagnosis 애그리거트 (순수 자바, 55개 테스트)
│       ├── model/       ProductProfile · ReadinessScore · Diagnosis · Evidence ...
│       ├── rule/        Condition(sealed) · Effect(sealed) · Rule · RuleSet
│       └── service/     RuleEvaluator · ScoreCalculator (순수 함수)
│   ├── application/ ✅ DiagnoseProductUseCase · 포트 · 폴백 오케스트레이션
│   │   ├── port/in/    DiagnoseProductUseCase · GetDiagnosisReportQuery
│   │   ├── port/out/   Load/Save · SearchEvidence · NarrateReport
│   │   └── service/    DiagnoseProductService · GetDiagnosisReportService (@UseCase)
│   └── adapter/     ✅ 양 끝 어댑터 완성 — 엔드투엔드 동작
│       ├── in/web/       DiagnosisController · 요청/응답 DTO · 웹 매퍼
│       └── out/
│           ├── ai/           스텁(@Profile local) + 실제 WebClient 어댑터(@Profile !local)
│           └── persistence/  JPA 애그리거트 매핑 · 룰 JSON 코덱 · 가중치 기준표
├── consulting/      ✅ 리드 접수 · 개인정보 AES-GCM 암호화 · 동의 로그
│   ├── domain/      ConsultingLead 애그리거트 · ContactInfo(마스킹) · ConsentRecord
│   ├── application/ RequestConsultingUseCase · 동의 검증 · 진단 존재 확인
│   └── adapter/     ConsultingController · 암호화 매퍼 · 리드/동의 영속성
├── auth/            인증·인가. JWT · 카카오/구글 OAuth · 이메일 인증 · 약관
├── board/           게시판 · 댓글 · 비밀글
├── document/        서류 발급 · 양식 · PDF
├── file/            업로드 저장소 · 경로 순회 방어 · 공개/비공개
├── notification/    알림
├── report/          리포트 문구 관리
├── audit/           관리자 변경 감사 로그
├── dashboard/       관리자 통계
└── cli/             ⭐ picocli 진입점. 스프링 없이 룰만 검증한다
```

바운디드 컨텍스트 11개입니다. 새 컨텍스트는 `common`의 구조를 그대로 따르고,
`domain` → `application` → `adapter` 순으로 만들면 의존 방향을 어길 수 없습니다.

### 진단 도메인 설계 원칙

- **`RuleEvaluator`·`ScoreCalculator`는 순수 함수다.** 스프링 빈이 아니며 `new`로 테스트한다.
  같은 입력은 항상 같은 결과를 낸다 — 항목 순서까지 동일하다.
- **판정은 룰이, 문장은 LLM이.** `Diagnosis.attachEvidence()`/`attachNarration()`은 후보·서류·점수를
  건드리지 않는다. RAG·LLM 실패는 진단 실패가 아니라 `degraded` 플래그다.
- **모르면 격리한다.** 후보가 없으면 `ExpertReviewItem(NO_MATCHING_RULE)`, 근거가 없으면
  `NO_EVIDENCE`, 입력이 부족하면 `AMBIGUOUS_CONDITION`. 지어내지 않는다.
- **재현성이 참조 무결성보다 우선.** 진단은 평가 시점의 룰셋 버전과 가중치를 스냅샷으로 들고 있다.

새 바운디드 컨텍스트는 `common`의 구조를 그대로 따릅니다.
`domain` → `application` → `adapter` 순으로 만들면 의존 방향을 어길 수 없습니다.

## 새 오류 코드 추가

컨텍스트마다 `ErrorCode`를 구현한 enum을 만듭니다. `HttpStatus`를 쓰지 마세요 —
도메인은 HTTP를 모릅니다. `ErrorType`이 웹 어댑터에서 상태 코드로 변환됩니다.

```java
public enum DiagnosisErrorCode implements ErrorCode {
    RULE_SET_NOT_FOUND("OCF-DIAG-001", "진단 규칙을 불러올 수 없습니다.", ErrorType.UNAVAILABLE);
    // ...
}
```

## 설정 주의

`opencertflow.blocking.jdbc-pool-size`와 `spring.datasource.hikari.maximum-pool-size`는
**같은 값**이어야 합니다. 어긋나면 시작 로그에 경고가 찍힙니다.
