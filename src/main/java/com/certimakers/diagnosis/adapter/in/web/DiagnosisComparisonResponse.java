package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ScoreView;
import com.certimakers.diagnosis.domain.service.DiagnosisComparison;
import com.certimakers.diagnosis.domain.model.DocumentCode;
import com.certimakers.diagnosis.domain.model.ReadinessScore;
import java.util.List;

/**
 * 재진단 비교 응답(F-APP-048).
 *
 * @param previousDiagnosisId   원 진단 ID
 * @param diagnosisId           재진단 ID
 * @param baselineScore         원 진단 준비도 점수
 * @param currentScore          재진단 준비도 점수
 * @param comparable            두 점수를 비교할 수 있는지. false면 변화량을 표시하면 안 된다
 * @param percentagePointChange 준비도 변화량(%p)
 * @param newlyHeldDocuments    이번에 새로 갖춘 서류
 * @param stillMissingDocuments 아직 갖추지 못한 서류
 * @param baselineDiffers       두 진단의 룰셋 버전이 다른지
 * @param notice                해석 주의 문구
 */
public record DiagnosisComparisonResponse(
        String previousDiagnosisId,
        String diagnosisId,
        ScoreView baselineScore,
        ScoreView currentScore,
        boolean comparable,
        int percentagePointChange,
        List<String> newlyHeldDocuments,
        List<String> stillMissingDocuments,
        boolean baselineDiffers,
        String notice) {

    private static final String NOTICE_BASELINE_DIFFERS =
            "두 진단의 판단 기준(룰셋)이 달라, 점수 차이를 준비도 개선으로 단정할 수 없습니다.";
    private static final String NOTICE_DEFAULT =
            "이 결과는 합격 예측이 아니라 사전 점검 지표입니다.";

    public static DiagnosisComparisonResponse from(DiagnosisComparison comparison) {
        var delta = comparison.delta();
        return new DiagnosisComparisonResponse(
                // id는 문자열로 준다 — 기존 응답들과 같은 방식(자바스크립트 정밀도 손실 회피).
                String.valueOf(comparison.previousId().value()),
                String.valueOf(comparison.currentId().value()),
                toScoreView(delta.before()),
                toScoreView(delta.after()),
                delta.comparable(),
                delta.percentagePointChange(),
                comparison.newlyHeld().stream().map(DocumentCode::value).toList(),
                comparison.stillMissing().stream().map(DocumentCode::value).toList(),
                comparison.baselineDiffers(),
                comparison.baselineDiffers() ? NOTICE_BASELINE_DIFFERS : NOTICE_DEFAULT);
    }

    private static ScoreView toScoreView(ReadinessScore score) {
        return new ScoreView(
                score.applicable(), score.percentage(), score.earnedWeight(), score.totalWeight());
    }
}