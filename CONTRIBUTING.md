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

## 인증 등급을 확인하는 절차

`[TODO-확인후]`가 붙은 룰을 실제 판정으로 바꾸려면 근거가 필요합니다. 아래는 전기방석에
실제로 적용해 등급을 확정한 절차입니다. 다른 품목도 같은 순서로 하면 됩니다.

### 1. 시행규칙 원문에서 별표를 대조한다 — 이것이 근거다

전기용품의 인증 등급은 별도 고시가 아니라 **「전기용품 및 생활용품 안전관리법 시행규칙」의
별표**에 있습니다. 법제처 API로 원문을 받습니다.

```bash
# 시행규칙의 법령일련번호(MST) 찾기
curl -s "https://www.law.go.kr/DRF/lawSearch.do?OC=$OC&target=law&type=JSON&display=1\
&query=$(printf '전기용품 및 생활용품 안전관리법 시행규칙' | jq -sRr @uri)"

# 본문 받기 (약 700KB)
curl -s "https://www.law.go.kr/DRF/lawService.do?OC=$OC&target=law&MST=286389&type=JSON" \
  -o rule.json
```

`$OC`는 [법제처에서 신청](https://open.law.go.kr/LSO/openApi/cuAskList.do)해 받은 인증값입니다.

별표 번호가 곧 등급입니다.

| 별표 | 등급 | 무게 |
|---|---|---|
| 별표 3 | 안전인증대상 | 제품시험 + **공장심사** |
| 별표 4 | 안전확인대상 | 시험성적서 제출 후 신고 |
| 별표 5 | 공급자적합성확인대상 | 제조·수입자가 스스로 확인 |

품목명을 찾고, **그 앞에 나오는 가장 가까운 별표 번호**를 보면 됩니다.

```bash
node -e '
const s=require("fs").readFileSync("rule.json","utf8");
let i=-1;
while((i=s.indexOf(process.argv[1], i+1))>=0){
  const before=s.slice(0,i);
  const byeol=[...before.matchAll(/별표\s*(\d+)\]/g)].pop();
  const grade=[...before.matchAll(/(안전인증|안전확인|공급자적합성확인)대상전기용품/g)].pop();
  console.log("별표 "+byeol[1]+" / "+grade[0]);
  console.log("  …"+s.slice(i-100,i+80).replace(/\s+/g," ")+"…");
}' 전기찜질기
```

**품목명은 법령의 용어를 써야 합니다.** 헤어드라이어는 시행규칙에 "헤어드라이어"로 없고
**모발관리기**로 있습니다. 못 찾으면 상위 분류어로 다시 찾아보세요.

### 2. 인증 등록 현황으로 교차 확인한다 — 근거가 아니라 확인이다

[제품안전정보센터 API](https://www.safetykorea.kr/release/openapi2)로 같은 품목이 실제로 어떤
등급으로 등록되어 왔는지 봅니다. 별표 해석이 맞았는지 검산하는 용도입니다.

```bash
curl -s -H "AuthKey: $SAFETYKOREA_KEY" \
  "https://www.safetykorea.kr/openapi/api/cert/certificationList.json\
?conditionKey=productName&conditionValue=$(printf '전기방석' | jq -sRr @uri)"
```

`certDiv` 필드에 법령과 등급이 함께 들어 있습니다. **구법(전기용품안전관리법) 건은 제외**하고
현행법 건만 보세요.

### ⚠️ 등록 현황을 근거로 삼지 마세요

**이 함정에 실제로 빠질 뻔했습니다.** 전기방석 846건을 보면 2023-10-12 이후 '안전확인'이 0건이고
그 뒤 72건이 전부 '안전인증'입니다. 여기까지만 보면 "전기방석 = 안전인증"이 됩니다.

틀렸습니다. 직류 제품이 **공급자적합성확인으로 옮겨간** 것이고, 그 제도는 인증기관을 거치지
않으므로 **등록부에 아예 나타나지 않습니다.** 사라진 것처럼 보였을 뿐입니다.

통계를 믿었다면 직류 제품 제조자에게 공장심사까지 필요한 안전인증을 안내할 뻔했습니다.

> 등록 현황은 **"무엇이 등록되었나"**를 말할 뿐 **"무엇이 필요한가"**를 말하지 않습니다.
> 근거는 언제나 법령 원문입니다.

### 3. 갈림길이 있으면 속성으로 만든다

같은 품목이 조건에 따라 다른 별표에 들어가는 일이 흔합니다. 전기찜질기가 그렇습니다.

```
별표 3  10) 교류전원을 사용하는 전기찜질기, 발 보온기      → 안전인증
별표 5  16) 직류전원을 사용하는 전기찜질기 및 발 보온기    → 공급자적합성확인
```

이럴 때 **다른 값으로 추론하지 마세요.** 정격전압으로 교류/직류를 가릴 수 없고(24V 교류와
24V 직류가 있습니다), 추론이 틀리면 그 사실이 조용히 묻힙니다. 갈림길이 되는 값은 입력으로
받습니다 — `PowerSource`가 그렇게 추가됐습니다.

그리고 **모를 때의 룰을 반드시 함께 만드세요.** 한쪽으로 뭉개면 과잉 안내(불필요한 비용)나
과소 안내(받아야 할 인증 누락) 중 하나가 됩니다.

```yaml
- code: R-EH-001P
  description: 전원 방식 모름 → 등급 판단 불가
  condition:
    { type: attr, attribute: POWER_SOURCE, operator: EQ, value: UNKNOWN }
  effects:
    - type: flagExpertReview
      reason: AMBIGUOUS_CONDITION
      question: 교류전원인지 직류전원인지에 따라 인증 등급이 완전히 달라집니다. …
```

### 4. 룰에 근거를 적는다

조항 번호를 YAML 주석과 `description`에 남기세요. 법령이 개정됐을 때 **무엇을 다시 봐야
하는지** 알 수 있어야 합니다.

```yaml
# 시행규칙 별표 3 · 10) 교류전원을 사용하는 전기찜질기, 발 보온기 → 안전인증대상
- code: R-EH-001
  description: 교류전원 + 신체 접촉 발열 제품 → 안전인증 대상 (시행규칙 별표 3 제10호)
```

### 5. 등급 분기를 테스트로 고정한다

등급을 틀리면 소공인이 필요 없는 비용을 쓰거나 받아야 할 인증을 놓칩니다. 반드시 테스트를
남기고, 단언에 **"이게 틀리면 무엇이 잘못되는가"**를 적으세요.

```java
assertThat(candidates)
        .as("직류 제품에 안전인증을 안내하면 필요 없는 공장심사 비용을 물리게 된다")
        .doesNotContain("SAFETY_CERT");
```

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
