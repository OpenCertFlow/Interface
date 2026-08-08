package io.opencertflow.diagnosis.application.port.out;

import io.opencertflow.diagnosis.domain.model.ProductGroup;
import io.opencertflow.diagnosis.domain.service.ScoreRubric;

/**
 * 아웃바운드 포트: 준비도 점수 가중치 기준표를 로드한다. 블로킹(JPA).
 *
 * <p>기준표가 비어 있어도 {@code null}이 아니라 {@link ScoreRubric#defaultsOnly()}를 반환해야 한다.
 * 점수 산정은 요구 강도 기본 가중치만으로도 동작하며, 여기서 빈 값을 흘리면 zip이 멈춘다.
 */
public interface LoadScoreRubricPort {

    ScoreRubric load(ProductGroup productGroup);
}
