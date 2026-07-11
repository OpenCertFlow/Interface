package com.certimakers.diagnosis.domain.service;

import com.certimakers.diagnosis.domain.model.CertificationCandidate;
import com.certimakers.diagnosis.domain.model.CertificationType;
import com.certimakers.diagnosis.domain.model.ExpertReviewItem;
import com.certimakers.diagnosis.domain.model.ExpertReviewReason;
import com.certimakers.diagnosis.domain.model.LabelingCheckItem;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.Requirement;
import com.certimakers.diagnosis.domain.model.SchemeCode;
import com.certimakers.diagnosis.domain.rule.AddCandidate;
import com.certimakers.diagnosis.domain.rule.AddLabelingCheck;
import com.certimakers.diagnosis.domain.rule.Effect;
import com.certimakers.diagnosis.domain.rule.FlagExpertReview;
import com.certimakers.diagnosis.domain.rule.RequireDocument;
import com.certimakers.diagnosis.domain.rule.Rule;
import com.certimakers.diagnosis.domain.rule.RuleCode;
import com.certimakers.diagnosis.domain.rule.RuleSet;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 룰 평가 도메인 서비스. 이 프로젝트의 핵심 자산이다.
 *
 * <p><b>순수 함수여야 한다.</b> 스프링 빈이 아니고, 상태가 없고, 시각·난수·I/O를 건드리지 않는다.
 * {@code new RuleEvaluator().evaluate(profile, ruleSet)}로 테스트할 수 있어야 하며, 같은 입력은
 * 항상 같은 결과를 낸다(ADR-0003, 04-domain-model.md).
 *
 * <p>효과는 누적적이다. 여러 룰이 같은 후보를 지목하면 매칭 룰을 병합하고, 같은 서류를 요구하면
 * 더 강한 요구가 이긴다. 어떤 후보도 나오지 않으면 결과를 억지로 지어내지 않고
 * {@link ExpertReviewReason#NO_MATCHING_RULE}로 격리한다(불변식 5).
 */
public class RuleEvaluator {

    public RuleEvaluationResult evaluate(ProductProfile profile, RuleSet ruleSet) {
        var candidateAccumulator = new CandidateAccumulator();
        Map<DocumentCode, Requirement> documents = new TreeMap<>(
                java.util.Comparator.comparing(DocumentCode::value));
        var labelingAccumulator = new LabelingAccumulator();
        // (question, reason) 조합으로 중복 제거하되 삽입 순서를 유지한다.
        Set<ExpertReviewItem> expertReviewItems = new LinkedHashSet<>();

        for (Rule rule : ruleSet.inPriorityOrder()) {
            if (!rule.matches(profile)) {
                continue;
            }
            for (Effect effect : rule.effects()) {
                applyEffect(effect, rule.code(), candidateAccumulator, documents,
                        labelingAccumulator, expertReviewItems);
            }
        }

        List<CertificationCandidate> candidates = candidateAccumulator.toCandidates();

        // 불변식 5: 후보가 하나도 없으면 침묵하지 않고 전문가 확인 필요로 전환한다.
        if (candidates.isEmpty()) {
            expertReviewItems.add(new ExpertReviewItem(
                    "입력하신 제품에 적용되는 인증 규칙을 찾지 못했습니다. 제품군과 사양을 전문가와 확인해 보세요.",
                    ExpertReviewReason.NO_MATCHING_RULE));
        }

        return new RuleEvaluationResult(
                ruleSet.version(),
                candidates,
                toRequiredDocuments(documents),
                labelingAccumulator.toItems(),
                List.copyOf(expertReviewItems));
    }

    private void applyEffect(
            Effect effect,
            RuleCode ruleCode,
            CandidateAccumulator candidates,
            Map<DocumentCode, Requirement> documents,
            LabelingAccumulator labeling,
            Set<ExpertReviewItem> expertReviewItems) {

        // Java 17에는 switch 패턴 매칭이 없어 instanceof 체인으로 분기한다. sealed 계층이므로 마지막
        // else는 이론상 도달 불가이며, 새 Effect 타입 추가 시 이 가드가 즉시 실패해 처리 누락을 알린다.
        if (effect instanceof AddCandidate add) {
            candidates.add(add.schemeCode(), add.type(), ruleCode);
        } else if (effect instanceof RequireDocument require) {
            documents.merge(require.documentCode(), require.requirement(), Requirement::strongerOf);
        } else if (effect instanceof AddLabelingCheck label) {
            labeling.add(label.label(), ruleCode);
        } else if (effect instanceof FlagExpertReview flag) {
            expertReviewItems.add(new ExpertReviewItem(flag.question(), flag.reason()));
        } else {
            throw new IllegalStateException("처리되지 않은 Effect 타입: " + effect.getClass().getName());
        }
    }

    private List<RequiredDocument> toRequiredDocuments(Map<DocumentCode, Requirement> documents) {
        return documents.entrySet().stream()
                .map(entry -> new RequiredDocument(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** 후보를 (제도, 유형) 단위로 모으고 매칭 룰을 병합한다. 삽입 순서를 유지한다. */
    private static final class CandidateAccumulator {
        private record Key(SchemeCode schemeCode, CertificationType type) {
        }

        private final Map<Key, Set<RuleCode>> byKey = new LinkedHashMap<>();

        void add(SchemeCode schemeCode, CertificationType type, RuleCode ruleCode) {
            byKey.computeIfAbsent(new Key(schemeCode, type), k -> new LinkedHashSet<>()).add(ruleCode);
        }

        List<CertificationCandidate> toCandidates() {
            return byKey.entrySet().stream()
                    .map(entry -> new CertificationCandidate(
                            entry.getKey().schemeCode(),
                            entry.getKey().type(),
                            sortedRuleCodes(entry.getValue())))
                    .toList();
        }
    }

    /** 라벨링 항목을 라벨 문자열 단위로 모으고 매칭 룰을 병합한다. */
    private static final class LabelingAccumulator {
        private final Map<String, Set<RuleCode>> byLabel = new LinkedHashMap<>();

        void add(String label, RuleCode ruleCode) {
            byLabel.computeIfAbsent(label, k -> new LinkedHashSet<>()).add(ruleCode);
        }

        List<LabelingCheckItem> toItems() {
            return byLabel.entrySet().stream()
                    .map(entry -> new LabelingCheckItem(entry.getKey(), sortedRuleCodes(entry.getValue())))
                    .toList();
        }
    }

    /** 매칭 룰 집합을 코드 사전순으로 고정한다. 결과의 순서까지 재현 가능하게 한다. */
    private static Set<RuleCode> sortedRuleCodes(Set<RuleCode> codes) {
        Set<RuleCode> sorted = new TreeSet<>(java.util.Comparator.comparing(RuleCode::value));
        sorted.addAll(codes);
        return sorted;
    }
}
