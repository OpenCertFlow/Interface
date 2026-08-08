package io.opencertflow.consulting.adapter.out.persistence;

import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.consulting.application.port.out.VerifyDiagnosisPort;
import io.opencertflow.consulting.domain.model.DiagnosisReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link VerifyDiagnosisPort} 구현. 진단 존재 여부만 확인한다.
 *
 * <p>진단 컨텍스트의 엔티티·리포지토리를 가져오는 대신 JdbcTemplate로 존재만 조회한다 — 컨텍스트
 * 간 결합을 코드 수준에서 만들지 않기 위함이다. 컨설팅은 진단의 내부 구조를 몰라도 된다.
 */
@PersistenceAdapter
public class VerifyDiagnosisAdapter implements VerifyDiagnosisPort {

    private final JdbcTemplate jdbcTemplate;

    public VerifyDiagnosisAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(DiagnosisReference reference) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM diagnosis WHERE id = ?)",
                Boolean.class,
                reference.value());
        return Boolean.TRUE.equals(exists);
    }
}
