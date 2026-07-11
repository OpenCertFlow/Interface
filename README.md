# 인증메이커스 — 백엔드

Spring Boot 3.5 (WebFlux) · Java 17 · PostgreSQL · 헥사고날 아키텍처

설계 문서는 [`docs/design/`](../docs/design/README.md)에 있습니다. 코드를 만지기 전에
[02-hexagonal-architecture.md](../docs/design/02-hexagonal-architecture.md)와
[ADR-0002](../docs/design/adr/0002-webflux-with-jpa.md)를 읽어 주세요.

## 실행

```bash
docker compose up -d postgres qdrant     # 프로젝트 루트에서
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

## 테스트

```bash
./gradlew test
```

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
com.certimakers
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
└── consulting/      ✅ 리드 접수 · 개인정보 AES-GCM 암호화 · 동의 로그
    ├── domain/      ConsultingLead 애그리거트 · ContactInfo(마스킹) · ConsentRecord
    ├── application/ RequestConsultingUseCase · 동의 검증 · 진단 존재 확인
    └── adapter/     ConsultingController · 암호화 매퍼 · 리드/동의 영속성
```

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
    RULE_SET_NOT_FOUND("CM-DIAG-001", "진단 규칙을 불러올 수 없습니다.", ErrorType.UNAVAILABLE);
    // ...
}
```

## 설정 주의

`certimakers.blocking.jdbc-pool-size`와 `spring.datasource.hikari.maximum-pool-size`는
**같은 값**이어야 합니다. 어긋나면 시작 로그에 경고가 찍힙니다.
