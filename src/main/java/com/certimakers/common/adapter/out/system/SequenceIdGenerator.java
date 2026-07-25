package com.certimakers.common.adapter.out.system;

import com.certimakers.common.domain.port.IdGenerator;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 전역 시퀀스({@code global_id_seq}) 기반 식별자 생성기.
 *
 * <p>애그리거트가 생성 시점에 id를 갖는 기존 설계를 유지하기 위해, DB가 삽입 시 id를 채우는
 * {@code @GeneratedValue} 대신 애플리케이션이 먼저 {@code nextval}로 값을 당겨 온다. 모든
 * 테이블이 한 시퀀스를 공유하므로 id는 전역적으로 유일하다.
 *
 * <p>{@code nextval} 호출은 블로킹 JDBC다. 모든 호출 지점이 {@code BlockingBridge}가 옮겨 준
 * 블로킹 스케줄러 위이므로 이벤트 루프를 막지 않는다(웹 어댑터의 직접 호출은 ArchUnit이 차단).
 */
@Component
public class SequenceIdGenerator implements IdGenerator {

    private final JdbcTemplate jdbcTemplate;

    public SequenceIdGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long nextId() {
        Long id = jdbcTemplate.queryForObject("SELECT nextval('global_id_seq')", Long.class);
        return Objects.requireNonNull(id, "global_id_seq가 값을 반환하지 않았습니다.");
    }
}
