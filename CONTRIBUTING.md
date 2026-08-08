# 기여 가이드

OpenCertFlow에 기여해 주셔서 감사합니다. 이 프로젝트는
[Apache License 2.0](LICENSE)으로 배포되며, 기여물도 같은 라이선스로 배포됩니다.
행동 규범은 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)를,
보안 취약점 신고는 [SECURITY.md](SECURITY.md)를 참고하세요.

## 인증 규칙에 기여하기 — 자바를 몰라도 됩니다

이 프로젝트에서 가장 값진 기여는 **규칙의 정확성**입니다. 규칙은 코드가 아니라 데이터이며,
[`rules/`](rules/)의 YAML만 고치면 됩니다. **DB도 서버도 IDE도 필요 없습니다.**

```bash
# 1. 규칙을 고친다
vi rules/electric-heating-pad/v1.yaml

# 2. 검증한다 (구조 · 값 타입 · 의미 3중)
./gradlew validateRules

# 3. 규칙이 어떻게 읽히는지 확인한다
./gradlew printRuntimeClasspath   # CP 확보
java -cp "$CP" io.opencertflow.cli.OpenCertFlowCli explain R-EH-005
```

편집기가 [`schema/ruleset.schema.json`](schema/ruleset.schema.json)을 읽어 자동완성과 실시간
검증을 해 줍니다(YAML 파일 첫 줄의 `yaml-language-server` 주석). CI도 같은 스키마를 씁니다.

### 규칙 기여에서 지켜야 할 것

**확인되지 않은 것을 단정하지 마세요.** 이 서비스의 존재 이유는 소공인이 틀린 정보로 재작업하는
일을 줄이는 것입니다. 인증 등급이나 요구 서류를 공식 자료로 확인하지 못했다면
`addCandidate`/`requireDocument`가 아니라 `flagExpertReview`로 보내세요. 모른다고 말하는 것은
실패가 아니라 정상 동작입니다.

```yaml
# ❌ 확인 안 된 등급을 단정
- { type: addCandidate, schemeCode: KC_SAFETY_CONFIRM_ELECTRIC, certificationType: SAFETY_CONFIRM }

# ✅ 확인이 필요하다고 말한다
- type: flagExpertReview
  reason: NO_EVIDENCE
  question: 신체에 닿는 발열 제품입니다. 적용되는 인증 제도와 등급을 인증기관에 확인해 주세요.
```

**근거를 PR 본문에 적어 주세요.** 어느 법령·고시·기관 안내의 몇 조인지 링크와 함께 남기면
리뷰가 빨라지고, 나중에 법령이 개정됐을 때 무엇을 다시 봐야 하는지 알 수 있습니다.

## 저장소 구성

| 저장소 | 역할 |
| --- | --- |
| [BackEnd](https://github.com/OpenCertFlow/BackEnd) | Spring WebFlux API 서버 |
| [MiddleWare](https://github.com/OpenCertFlow/MiddleWare) | RAG 워커 (색인·검색·서술) |
| [FrontEnd](https://github.com/OpenCertFlow/FrontEnd) | Kotlin·Compose Android 앱 |

작업 현황은 [프로젝트 보드](https://github.com/orgs/OpenCertFlow/projects/1)에서 확인한다.

## 작업 흐름

1. **이슈 생성** — 템플릿(✨ Feature / 🐛 Bug / 🛠 Task) 중 하나를 고른다.
2. **브랜치 생성** — 이슈 번호를 붙인다.
3. **커밋** — 아래 커밋 규칙을 따른다.
4. **PR 생성** — PR 템플릿을 채우고 관련 이슈를 연결한다.
5. **리뷰 → 머지** — 리뷰어 승인 후 머지한다.

## 이슈 규칙

제목은 `[ 페이지명 ] 내용` 형식으로 쓴다. **대괄호 안에 띄어쓰기가 있다.**

```
[ Main ] 메인 뷰 구현
[ 리포트 ] 진단 결과 리포트 UI 구현
[ CI ] GitHub Actions 파이프라인 구성
```

본문은 템플릿의 `💚 어떤 기능인가요?` / `✅ To Dos` 항목을 채운다.
To Dos는 최대한 세분화하고, 구현 완료 시 캡처를 남긴다.

## 브랜치 전략

`main` 하나를 기준으로 하는 단순 트렁크 방식이다.

```
<타입>/<이슈번호>-<간단한-설명>

feat/12-product-input-screen
fix/34-report-crash
chore/7-ci-pipeline
```

- `main`에 **직접 푸시하지 않는다.** 반드시 PR로 머지한다.
- 머지 후 작업 브랜치는 삭제한다.

> `main` 직접 푸시 차단·CI 통과 필수는 GitHub 브랜치 보호 규칙으로 강제하는 것이
> 이상적이나, 현재 저장소가 비공개 + 무료 플랜이라 해당 기능을 쓸 수 없다.
> 따라서 **팀 합의로 지킨다.**

## 커밋 규칙

```
<TYPE>: <한글 요약>

<본문 — 무엇을 왜 바꿨는지>
```

| 타입 | 용도 |
| --- | --- |
| `FEAT` | 새 기능 |
| `FIX` | 버그 수정 |
| `CHORE` | 빌드·설정·인프라 등 기능 외 작업 |
| `TEST` | 테스트 추가·수정 |
| `DOCUMENT` | 문서 작업 |
| `REFACTOR` | 동작 변경 없는 구조 개선 |

- 한 커밋은 **하나의 관심사**만 담는다.
- 요약은 명령형이 아닌 서술형 한글로 쓴다. (예: `FEAT: 제품군 메타데이터 API 추가`)
- 본문에 관련 이슈를 남긴다. (`관련 #12`)

## PR 규칙

- 제목은 커밋 규칙과 동일한 형식을 쓴다.
- PR 템플릿의 **체크리스트를 모두 확인**한다.
- 관련 이슈를 연결한다. (`Closes #12`)
- CODEOWNERS가 자동으로 리뷰어로 지정된다.
- **CI가 통과해야 머지한다.**

## 로컬 검증

PR 올리기 전에 아래를 통과시킨다.

```bash
./gradlew test
```

단위 테스트, ArchUnit(아키텍처 규칙), BlockHound(블로킹 호출 탐지),
Testcontainers 통합 테스트가 함께 돈다.
ArchUnit이 빠지면 단일 모듈 헥사고날의 전제가 무너지므로(ADR-0001) 우회하지 않는다.

## 지켜야 할 것

- **개인정보·시크릿을 커밋에 포함하지 않는다.** API 키·연락처·인증 정보는 환경변수로 다룬다.
- 공식 출처가 없는 인증 정보를 사용자에게 단정적으로 노출하지 않는다.
- 진단 결과는 **합격 예측이 아니라 사전 점검 지표**임을 항상 명시한다.
