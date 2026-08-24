package io.opencertflow.diagnosis.adapter.in.web.prep;

import io.opencertflow.diagnosis.domain.model.PrepPlan;
import java.time.Instant;
import java.util.List;

/**
 * 준비 현황 응답(F-APP-049).
 *
 * @param hasItems 준비할 항목이 있는지. false면 progress 0을 "아무것도 안 했다"로 읽으면 안 된다
 * @param notice   화면에 함께 띄울 안내 문구
 */
public record PrepPlanResponse(
        String diagnosisId,
        int completed,
        int total,
        int progress,
        boolean hasItems,
        List<PrepItemView> items,
        String notice,
        Instant updatedAt) {

    /** 항목 하나. 표시 순서는 진단이 정한 보완 우선순위(가중치 내림차순) 그대로다. */
    public record PrepItemView(String documentCode, boolean done) {
    }

    private static final String NOTICE_NO_ITEMS = "추가로 준비할 서류가 없습니다.";
    private static final String NOTICE_DEFAULT = "이 결과는 합격 예측이 아니라 사전 점검 지표입니다.";

    public static PrepPlanResponse from(PrepPlan plan) {
        return new PrepPlanResponse(
                String.valueOf(plan.diagnosisId().value()),   // id는 문자열로 — 다른 응답과 같은 방식
                plan.completed(),
                plan.total(),
                plan.progress(),
                plan.hasItems(),
                plan.items().stream()
                        .map(item -> new PrepItemView(item.documentCode().value(), item.done()))
                        .toList(),
                plan.hasItems() ? NOTICE_DEFAULT : NOTICE_NO_ITEMS,
                plan.updatedAt());
    }
}
