# draw.io 다이어그램

[certimakers.drawio](certimakers.drawio) — 편집 가능한 네이티브 draw.io(mxGraph) 파일. 6개 페이지.

| 페이지 | 대응 문서 |
| --- | --- |
| 01. 시스템 아키텍처 | [01-system-architecture.md](../01-system-architecture.md) |
| 02. 헥사고날 의존 방향 | [02-hexagonal-architecture.md](../02-hexagonal-architecture.md) |
| 03. 진단 시퀀스 | [03-diagnosis-flow.md](../03-diagnosis-flow.md) |
| 04. 장애 폴백 정책 | [03-diagnosis-flow.md](../03-diagnosis-flow.md#34-장애-폴백-정책) |
| 05. 진단 상태 전이 | [03-diagnosis-flow.md](../03-diagnosis-flow.md#35-상태-전이) |
| 06. ERD (PostgreSQL) | [05-data-model.md](../05-data-model.md) |

## 여는 방법

- **웹**: [app.diagrams.net](https://app.diagrams.net) → Open Existing Diagram → 이 파일 선택
- **VS Code**: `Draw.io Integration` 확장(hediet.vscode-drawio) 설치 후 파일 클릭
- **데스크톱**: draw.io Desktop 앱에서 열기

하단 탭으로 6개 페이지를 전환합니다. 모든 도형·화살표는 draw.io에서 그대로 편집·재배치할 수 있습니다.

## 색상 규약

| 색 | 의미 |
| --- | --- |
| 초록 | 도메인 / 결정론 영역 — 같은 입력이면 같은 결과 |
| 파랑 | 애플리케이션 계층 |
| 주황(점선) | 확률론 영역(RAG·LLM) — 실패해도 진단은 완료 |
| 빨강 | 외부 시스템 · 진단 실패 지점 |
| 노랑 | 읽는 사람이 놓치기 쉬운 설계 의도 주석 |

## 두 형식을 함께 두는 이유

Markdown 안의 Mermaid는 GitHub·IDE에서 바로 렌더되어 **코드 리뷰와 diff**에 좋습니다. draw.io 파일은
**발표 자료와 자유로운 편집**에 좋습니다. 설계가 바뀌면 두 곳을 함께 갱신합니다 — 진실의 원천은
어디까지나 각 Markdown 문서이고, draw.io는 그것의 시각화 사본입니다.
