package com.certimakers.consulting.application.port.in;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 컨설턴트 상담 처리(F-WCON). 리드 조회·담당 배정·상태 전이·내부 메모를 다룬다.
 *
 * <p>연락처는 컨설턴트가 상담을 위해 볼 수 있다 — 저장 시 암호화되지만 조회 시 복호화해 제공한다.
 */
public interface ManageConsultingUseCase {

    Mono<List<LeadSummary>> list(String statusFilter, int limit);

    Mono<LeadDetail> get(String leadId);

    Mono<LeadDetail> assign(String leadId, String consultantId);

    Mono<LeadDetail> changeStatus(String leadId, String status);

    Mono<LeadDetail> updateMemo(String leadId, String memo);

    record LeadSummary(String id, String diagnosisId, String contactName, String status,
                       String assignedConsultantId, Instant createdAt) {
    }

    record LeadDetail(String id, String diagnosisId, String contactName, String contactPhone,
                      String contactEmail, String message, String status,
                      String assignedConsultantId, String internalMemo, Instant createdAt) {
    }
}
