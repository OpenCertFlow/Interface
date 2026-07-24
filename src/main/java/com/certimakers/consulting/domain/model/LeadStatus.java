package com.certimakers.consulting.domain.model;

import java.util.Set;

/**
 * 컨설팅 리드 처리 상태와 허용 전이. 컨설턴트 워크플로가 이 전이 규칙을 따른다.
 *
 * <p>SUBMITTED(접수) → ASSIGNED(배정) → IN_PROGRESS(진행) → COMPLETED(완료). 종료 전 어느 단계에서든
 * CANCELLED(취소)로 갈 수 있다. COMPLETED·CANCELLED는 종착 상태다.
 */
public enum LeadStatus {

    SUBMITTED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(LeadStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    private Set<LeadStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> Set.of(ASSIGNED, CANCELLED);
            case ASSIGNED -> Set.of(IN_PROGRESS, COMPLETED, CANCELLED);
            case IN_PROGRESS -> Set.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> Set.of();
        };
    }
}
