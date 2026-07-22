package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.CandidateView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ChecklistView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ExpertReviewView;
import com.certimakers.diagnosis.adapter.in.web.DiagnosisReportResponse.ScoreView;
import java.util.List;

/**
 * 시뮬레이션 응답. "가정을 적용하면 이렇게 된다"(결과)와 "원본과 무엇이 달라졌다"(델타)를 함께 준다.
 *
 * @param diagnosisId              기준이 된 원본 진단 ID
 * @param baselineScore            원본 준비도 점수
 * @param simulatedScore           가정 적용 후 준비도 점수
 * @param comparable               두 점수를 비교할 수 있는지. false면 변화량을 표시하면 안 된다
 * @param percentagePointChange    준비도 변화량(%p)
 * @param certificationScopeChanged 적용 인증 제도 자체가 바뀌었는지
 * @param addedCandidates          새로 생긴 인증 후보
 * @param removedCandidates        더 이상 해당하지 않는 인증 후보
 * @param newlyRequiredDocuments   새로 요구된 서류
 * @param noLongerRequiredDocuments 더 이상 요구되지 않는 서류
 * @param newlySatisfiedDocuments  이번 가정으로 충족된 서류
 * @param checklist                가정 적용 후 체크리스트 전체
 * @param remediationOrder         가정 적용 후 보완 우선순위
 * @param expertReviewItems        가정 적용 후 전문가 확인 필요 항목
 * @param ruleSetVersion           시뮬레이션에 사용한 룰셋 버전
 * @param notice                   시뮬레이션의 성격을 알리는 고지 문구
 */
public record SimulationResponse(
        String diagnosisId,
        ScoreView baselineScore,
        ScoreView simulatedScore,
        boolean comparable,
        int percentagePointChange,
        boolean certificationScopeChanged,
        List<CandidateView> addedCandidates,
        List<CandidateView> removedCandidates,
        List<String> newlyRequiredDocuments,
        List<String> noLongerRequiredDocuments,
        List<String> newlySatisfiedDocuments,
        List<ChecklistView> checklist,
        List<ChecklistView> remediationOrder,
        List<ExpertReviewView> expertReviewItems,
        int ruleSetVersion,
        String notice) {
}
