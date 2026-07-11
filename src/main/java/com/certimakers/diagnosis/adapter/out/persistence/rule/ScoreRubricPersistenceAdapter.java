package com.certimakers.diagnosis.adapter.out.persistence.rule;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.diagnosis.application.port.out.LoadScoreRubricPort;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import com.certimakers.diagnosis.domain.service.ScoreRubric;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LoadScoreRubricPort} 구현. 가중치 기준표를 읽어 {@link ScoreRubric}으로 만든다.
 *
 * <p>기준표가 비어 있어도 {@link ScoreRubric#defaultsOnly()}를 반환한다 — 점수 산정은 요구 강도
 * 기본 가중치만으로도 동작하며, 빈 값을 흘리면 서비스의 zip이 멈춘다.
 */
@PersistenceAdapter
public class ScoreRubricPersistenceAdapter implements LoadScoreRubricPort {

    private final DocumentWeightJpaRepository repository;

    public ScoreRubricPersistenceAdapter(DocumentWeightJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreRubric load(ProductGroup productGroup) {
        Map<DocumentCode, Integer> weights = repository.findAll().stream()
                .collect(Collectors.toMap(
                        entity -> DocumentCode.of(entity.getDocumentCode()),
                        DocumentWeightEntity::getWeight));
        return new ScoreRubric(weights);
    }
}
