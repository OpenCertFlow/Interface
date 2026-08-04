package com.certimakers.diagnosis.domain.service;

import com.certimakers.common.domain.model.Guard;
import com.certimakers.diagnosis.domain.model.CertificationCandidate;
import com.certimakers.diagnosis.domain.model.ExpertReviewItem;
import com.certimakers.diagnosis.domain.model.LabelingCheckItem;
import com.certimakers.diagnosis.domain.rule.RuleCode;
import com.certimakers.diagnosis.domain.rule.RuleTrace;
import com.certimakers.diagnosis.domain.rule.RuleSetVersion;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 룰 평가의 결과. 결정론 영역의 출력이며, 점수 산정과 근거 검색의 입력이 된다.
 *
 * <p>모든 목록은 결정적으로 정렬되어 있다. 같은 입력이면 항목의 순서까지 동일하다.
 *
 * @param ruleSetVersion   평가에 사용한 룰셋 버전 (재현용 스냅샷)
 * @param candidates       인증 후보. 비어 있을 수 있으며, 그 경우 expertReviewItems에 사유가 담긴다
 * @param requiredDocuments 요구 서류 (가중치·보유 여부 미결합)
 * @param labelingChecks   표시·라벨링 확인 항목
 * @param expertReviewItems 전문가 확인 필요 항목
 * @param traces           발동한 룰과 그 이유. 결과를 되짚기 위한 기록이다
 */
public record RuleEvaluationResult(
        RuleSetVersion ruleSetVersion,
        List<CertificationCandidate> candidates,
        List<RequiredDocument> requiredDocuments,
        List<LabelingCheckItem> labelingChecks,
        List<ExpertReviewItem> expertReviewItems,
        List<RuleTrace> traces) {

    public RuleEvaluationResult {
        Guard.notNull(ruleSetVersion, "ruleSetVersion");
        candidates = List.copyOf(Guard.notNull(candidates, "candidates"));
        requiredDocuments = List.copyOf(Guard.notNull(requiredDocuments, "requiredDocuments"));
        labelingChecks = List.copyOf(Guard.notNull(labelingChecks, "labelingChecks"));
        expertReviewItems = List.copyOf(Guard.notNull(expertReviewItems, "expertReviewItems"));
        traces = traces == null ? List.of() : List.copyOf(traces);
    }

    /** 트레이스 개념이 없던 호출부·테스트를 그대로 두기 위한 생성자. */
    public RuleEvaluationResult(
            RuleSetVersion ruleSetVersion,
            List<CertificationCandidate> candidates,
            List<RequiredDocument> requiredDocuments,
            List<LabelingCheckItem> labelingChecks,
            List<ExpertReviewItem> expertReviewItems) {
        this(ruleSetVersion, candidates, requiredDocuments, labelingChecks,
                expertReviewItems, List.of());
    }

    public boolean hasCandidate() {
        return !candidates.isEmpty();
    }

    /** 이 진단에서 매칭된 모든 룰 코드. 후보를 만든 룰과 라벨링을 만든 룰을 합친다. */
    public Set<RuleCode> allMatchedRules() {
        return java.util.stream.Stream.concat(
                        candidates.stream().flatMap(candidate -> candidate.matchedRules().stream()),
                        labelingChecks.stream().flatMap(labeling -> labeling.matchedRules().stream()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
