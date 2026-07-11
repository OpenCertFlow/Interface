package com.certimakers.diagnosis.adapter.in.web;

import java.util.List;

/**
 * 진단 리포트 응답. 결과 화면(준비도 점수, 누락 서류, 표시·라벨링, 우선 보완 순서, 쉬운 설명,
 * 컨설팅 연결)에 필요한 모든 항목을 담는다(기획서).
 */
public record DiagnosisReportResponse(
        String id,
        String status,
        ScoreView score,
        List<CandidateView> candidates,
        List<ChecklistView> checklist,
        List<ChecklistView> remediationOrder,
        List<String> labelingChecks,
        List<ExpertReviewView> expertReviewItems,
        List<EvidenceView> evidences,
        NarrationView narration,
        DegradedView degraded) {

    /** 준비도 점수. applicable=false면 percentage는 의미 없다(산정 불가). */
    public record ScoreView(boolean applicable, int percentage, int earnedWeight, int totalWeight) {
    }

    public record CandidateView(String schemeCode, String certificationType, List<String> matchedRules) {
    }

    public record ChecklistView(String documentCode, String requirement, int weight, boolean held) {
    }

    public record ExpertReviewView(String question, String reason) {
    }

    public record EvidenceView(String sectionType, String snippet, String sourceUrl, double relevance) {
    }

    public record NarrationView(
            String summary,
            List<String> nextActions,
            List<String> preConsultQuestions,
            String disclaimer,
            boolean templateFallback) {
    }

    /** 어느 부분이 저하됐는지. 클라이언트가 "근거를 불러오지 못했습니다" 배너를 띄우는 근거. */
    public record DegradedView(boolean evidence, boolean narration) {
    }
}
