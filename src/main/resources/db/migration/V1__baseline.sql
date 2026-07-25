-- V1: 베이스라인. 확장·전역 시퀀스·공통 설정만 선언한다.
-- 실제 테이블은 V2 이후에서 만든다. docs/design/05-data-model.md 참조.

-- 외부 시스템(Qdrant 포인트 id 등)에서 여전히 UUID가 필요하므로 pgcrypto는 유지한다.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 전역 식별자 시퀀스. 모든 애그리거트가 이 하나를 공유하므로 id는 테이블을 넘어 전역 유일하다.
-- 애플리케이션(IdGenerator)과 일부 자식 엔티티의 @GeneratedValue(SEQUENCE)가 함께 nextval을 당겨 쓴다.
-- 시드 데이터는 1_000_000 미만의 고정 id를 쓰므로, 시퀀스를 그 위에서 시작해 충돌을 피한다.
CREATE SEQUENCE IF NOT EXISTS global_id_seq START WITH 1000000 INCREMENT BY 1;

-- 진단 결과의 재현성을 위해 모든 시각은 timestamptz(UTC)로 저장한다.
-- 애플리케이션은 java.time.Instant로 읽고 쓴다.
SET timezone = 'UTC';
