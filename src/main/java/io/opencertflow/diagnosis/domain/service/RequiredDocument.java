package io.opencertflow.diagnosis.domain.service;

import io.opencertflow.common.domain.model.Guard;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.Requirement;

/**
 * 룰 평가가 산출한 "요구 서류" 한 건. 아직 가중치와 보유 여부가 붙지 않은 중간 산출물이다.
 *
 * <p>{@code ScoreCalculator}가 여기에 기준표의 가중치와 사용자의 보유 여부를 결합해
 * {@link io.opencertflow.diagnosis.domain.model.ChecklistItem}으로 완성한다. 룰 평가와 점수 산정의
 * 책임을 나누는 경계다.
 */
public record RequiredDocument(DocumentCode documentCode, Requirement requirement) {

    public RequiredDocument {
        Guard.notNull(documentCode, "documentCode");
        Guard.notNull(requirement, "requirement");
    }
}
