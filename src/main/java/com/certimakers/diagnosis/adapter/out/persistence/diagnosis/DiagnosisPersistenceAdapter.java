package com.certimakers.diagnosis.adapter.out.persistence.diagnosis;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.diagnosis.application.port.out.LoadDiagnosisPort;
import com.certimakers.diagnosis.application.port.out.SaveDiagnosisPort;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import org.springframework.transaction.annotation.Transactional;

/**
 * 진단 애그리거트의 저장·로드 어댑터. {@link SaveDiagnosisPort}와 {@link LoadDiagnosisPort}를 함께
 * 구현한다 — 둘 다 같은 애그리거트, 같은 매퍼, 같은 리포지토리를 쓰기 때문이다.
 *
 * <p>블로킹(JPA)이며 트랜잭션 경계가 이 클래스 안에 있다. 서비스는 {@code BlockingBridge}로 감싼다(ADR-0002).
 */
@PersistenceAdapter
public class DiagnosisPersistenceAdapter implements SaveDiagnosisPort, LoadDiagnosisPort {

    private final DiagnosisJpaRepository repository;
    private final DiagnosisMapper mapper;

    public DiagnosisPersistenceAdapter(DiagnosisJpaRepository repository, DiagnosisMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Diagnosis save(Diagnosis diagnosis) {
        DiagnosisEntity entity = mapper.toEntity(diagnosis);
        repository.save(entity);
        return diagnosis; // 저장 성공 시 도메인 객체를 그대로 돌려준다(생성 필드 없음)
    }

    @Override
    @Transactional(readOnly = true)
    public Diagnosis load(DiagnosisId id) {
        // 트랜잭션 안에서 매핑하며 자식 컬렉션을 지연 초기화한다.
        return repository.findById(id.value())
                .map(mapper::toDomain)
                .orElse(null);
    }
}
