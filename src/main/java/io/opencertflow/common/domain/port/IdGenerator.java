package io.opencertflow.common.domain.port;

/**
 * 식별자 생성. 아웃바운드 포트다.
 *
 * <p>기본 구현은 전역 DB 시퀀스({@code global_id_seq})에서 값을 받아온다. 모든 애그리거트가
 * 하나의 시퀀스를 공유하므로 식별자는 테이블을 넘어 전역적으로 유일하고, 단조 증가라 삽입이 항상
 * 인덱스 오른쪽 끝에 몰려 페이지 분할이 적다. 이 선택을 도메인에서 감추기 위해 포트로 둔다.
 *
 * <p>구현이 블로킹 JDBC({@code nextval})를 호출하므로 <b>블로킹 스케줄러</b>에서만 부른다.
 * 이벤트 루프(웹 어댑터)에서의 호출은 ArchUnit이 막는다.
 */
public interface IdGenerator {

    Long nextId();
}
