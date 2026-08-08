# 보안 정책 (Security Policy)

## 지원 버전

이 프로젝트는 아직 `0.x` 단계입니다. 보안 수정은 `main` 브랜치에만 반영됩니다.

| 버전 | 지원 |
|---|---|
| `main` | ✅ |
| `0.0.x` 태그 | ❌ |

## 취약점 신고

**공개 이슈로 올리지 마세요.** GitHub의
[Private vulnerability reporting](https://github.com/OpenCertFlow/BackEnd/security/advisories/new)을
이용해 주세요.

신고에 아래를 포함해 주시면 확인이 빠릅니다.

- 영향 범위(어떤 엔드포인트·어떤 데이터)
- 재현 절차
- 확인한 커밋 해시 또는 버전

접수 후 **3영업일 이내**에 1차 회신하고, 유효한 취약점은 수정 후 GitHub Security
Advisory로 공개합니다. 원하시면 크레딧에 성함을 남깁니다.

## 이 프로젝트에서 민감한 것

진단 서비스 특성상 아래 데이터를 다룹니다. 관련 취약점은 우선순위가 높습니다.

| 데이터 | 보호 방식 |
|---|---|
| 상담 신청자 연락처 | AES-GCM 암호화 저장 (`TextEncryptor`) |
| 진단 입력(제품 사양·제조 방식) | 소유자 기반 접근 제어 |
| 로그 | 이메일·휴대폰·주민등록번호 정규식 마스킹 (`SensitiveDataMasker`) |

## 운영 배포 시 필수 설정

아래를 주입하지 않으면 **안전하지 않은 기본값으로 기동합니다.**

| 환경변수 | 미설정 시 |
|---|---|
| `OPENCERTFLOW_AUTH_JWT_SECRET` | 저장소에 공개된 기본 시크릿 사용 → **토큰 위조 가능** |
| `OPENCERTFLOW_SECURITY_ENCRYPTION_KEY` | 임시 키 생성 → 재시작 시 기존 개인정보 복호화 불가 |

`prod` 프로파일에서는 두 값이 없으면 기동이 실패합니다.
