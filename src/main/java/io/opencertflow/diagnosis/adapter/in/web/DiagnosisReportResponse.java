package io.opencertflow.diagnosis.adapter.in.web;

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
        DocumentSummaryView documentSummary,
        List<String> labelingChecks,
        List<ExpertReviewView> expertReviewItems,
        List<EvidenceView> evidences,
        List<RuleTraceView> ruleTraces,
        NarrationView narration,
        DegradedView degraded) {

    /** 준비도 점수. applicable=false면 percentage는 의미 없다(산정 불가). */
    public record ScoreView(boolean applicable, int percentage, int earnedWeight, int totalWeight) {
    }

    public record CandidateView(String schemeCode, String certificationType, List<String> matchedRules) {
    }

    /**
     * @param status HELD · MISSING · UNKNOWN
     * @param held   status == HELD. 기존 클라이언트 호환용 파생값
     */
    public record ChecklistView(
            String documentCode, String requirement, int weight, String status, boolean held) {

        public ChecklistView(String documentCode, String requirement, int weight, String status) {
            this(documentCode, requirement, weight, status, "HELD".equals(status));
        }
    }

    /** 서류 준비 현황 요약. 화면이 "누락 3건 · 확인 중 2건"을 바로 그릴 수 있게 한다. */
    public record DocumentSummaryView(int required, int held, int absent, int unknown) {
    }

    public record ExpertReviewView(String question, String reason) {
    }

    /**
     * 룰이 발동한 이유. "R-EH-001이 걸렸다"가 아니라 "전기 사용=예, 신체접촉=직접피부여서 걸렸다"를
     * 보여 주기 위한 것이다(기획서 3.2 설명가능성).
     */
    public record RuleTraceView(
            String ruleCode, int priority, List<FactView> facts, List<String> effects) {
    }

    /**
     * @param negated 부정 조건으로 만족했는지. "직접 접촉이 아닐 것"처럼 읽어야 하는 경우 참
     */
    public record FactView(
            String attribute, String operator, String expected, String actual, boolean negated) {
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
