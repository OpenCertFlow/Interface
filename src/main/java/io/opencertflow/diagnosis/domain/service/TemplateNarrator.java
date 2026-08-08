package io.opencertflow.diagnosis.domain.service;

import io.opencertflow.diagnosis.domain.model.ChecklistItem;
import io.opencertflow.diagnosis.domain.model.Diagnosis;
import io.opencertflow.diagnosis.domain.model.ExpertReviewItem;
import io.opencertflow.diagnosis.domain.model.Narration;
import io.opencertflow.diagnosis.domain.model.ReadinessScore;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM이 없을 때 규칙 결과만으로 리포트 문장을 조립하는 폴백 문장기. 순수 함수다.
 *
 * <p>이것이 있기에 LLM 호출 실패가 진단 실패가 아니다. 문장은 투박하지만 판정·점수·서류·근거는
 * 그대로 유효하며, 사용자는 여전히 다음 행동을 알 수 있다(ADR-0003). 여기서 만든 Narration은
 * {@code templateFallback = true}이며, 이 값이 진단을 COMPLETED_DEGRADED로 만든다.
 */
public class TemplateNarrator {

    private static final String MODEL_ID = "template";
    private static final String DISCLAIMER =
            "본 결과는 인증 합격·불합격 판정이 아니라 준비 상태를 확인하기 위한 사전 점검 지표입니다. "
                    + "정확한 판단은 인증 전문가 상담을 통해 확인하세요.";

    public Narration narrate(Diagnosis diagnosis) {
        return new Narration(
                buildSummary(diagnosis),
                buildNextActions(diagnosis),
                buildPreConsultQuestions(diagnosis.expertReviewItems()),
                DISCLAIMER,
                MODEL_ID,
                true);
    }

    private String buildSummary(Diagnosis diagnosis) {
        String productName = diagnosis.profile().productName();
        if (diagnosis.candidates().isEmpty()) {
            return "%s에 적용되는 인증 규칙을 찾지 못했습니다. 제품군과 사양을 전문가와 확인해 보세요."
                    .formatted(productName);
        }
        String candidateNames = diagnosis.candidates().stream()
                .map(candidate -> candidate.type().displayName())
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        ReadinessScore score = diagnosis.score();
        String scorePart = score.applicable()
                ? "현재 준비도는 %d%%입니다.".formatted(score.percentage())
                : "요구 서류가 확인되지 않아 준비도를 산정할 수 없습니다.";
        return "%s은(는) %s 검토 대상으로 보입니다. %s".formatted(productName, candidateNames, scorePart);
    }

    private List<String> buildNextActions(Diagnosis diagnosis) {
        List<String> actions = new ArrayList<>();
        for (ChecklistItem missing : missingByPriority(diagnosis)) {
            actions.add("%s 서류를 준비하세요. (%s)"
                    .formatted(missing.documentCode().value(), missing.requirement().displayName()));
        }
        if (actions.isEmpty()) {
            actions.add("필요한 서류를 모두 보유하고 있습니다. 인증 전문가 상담으로 다음 단계를 확인하세요.");
        }
        return actions;
    }

    private List<ChecklistItem> missingByPriority(Diagnosis diagnosis) {
        return diagnosis.checklist().stream()
                .filter(ChecklistItem::isMissing)
                .sorted(java.util.Comparator.comparingInt(ChecklistItem::weight).reversed()
                        .thenComparing(item -> item.documentCode().value()))
                .toList();
    }

    private List<String> buildPreConsultQuestions(List<ExpertReviewItem> expertReviewItems) {
        return expertReviewItems.stream().map(ExpertReviewItem::question).toList();
    }
}
