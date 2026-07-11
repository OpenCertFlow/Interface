-- V1: 베이스라인. 확장과 공통 도메인만 선언한다.
-- 실제 테이블은 V2 이후에서 만든다. docs/design/05-data-model.md 참조.

-- gen_random_uuid() 등 암호 관련 함수. 애플리케이션이 UUIDv7을 생성하지만,
-- 시드 데이터와 마이그레이션 스크립트에서 필요하다.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 진단 결과의 재현성을 위해 모든 시각은 timestamptz(UTC)로 저장한다.
-- 애플리케이션은 java.time.Instant로 읽고 쓴다.
SET timezone = 'UTC';
