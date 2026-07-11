package com.certimakers.common.domain.port;

import java.util.UUID;

/**
 * 식별자 생성. 아웃바운드 포트다.
 *
 * <p>기본 구현은 UUIDv7(시간 정렬)이다. 랜덤 UUIDv4를 PK로 쓰면 삽입이 B-tree 전역에 흩어져
 * 페이지 분할이 잦아지지만, 시간 정렬 UUID는 항상 인덱스 오른쪽 끝에 삽입된다.
 * 이 선택을 도메인에서 감추기 위해 포트로 둔다.
 */
public interface IdGenerator {

    UUID nextId();
}
