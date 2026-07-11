package com.certimakers.diagnosis.domain.service;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.Requirement;
import java.util.Map;

/**
 * 준비도 점수 가중치 기준표. 서류별 가중치를 정의한다.
 *
 * <p>가중치를 코드에 하드코딩하지 않고 이 객체(→ {@code document_weight} 테이블)로 주입하는 이유는,
 * 점수 기준표가 산출물이고 심사에서 근거를 요구받기 때문이다. 특정 서류에 명시적 가중치가 없으면
 * {@link Requirement#defaultWeight()}로 폴백한다.
 */
public record ScoreRubric(Map<DocumentCode, Integer> weights) {

    public ScoreRubric {
        weights = Map.copyOf(Guard.notNull(weights, "weights"));
        weights.forEach((code, weight) ->
                Guard.positive(weight, "weight[" + code.value() + "]"));
    }

    /** 명시적 가중치가 없으면 요구 강도의 기본 가중치를 쓴다. */
    public int weightOf(DocumentCode documentCode, Requirement requirement) {
        Integer explicit = weights.get(documentCode);
        return explicit != null ? explicit : requirement.defaultWeight();
    }

    /** 기준표가 아직 없을 때(초기 개발·테스트) 요구 강도 기본값만으로 동작하는 룹릭. */
    public static ScoreRubric defaultsOnly() {
        return new ScoreRubric(Map.of());
    }
}
