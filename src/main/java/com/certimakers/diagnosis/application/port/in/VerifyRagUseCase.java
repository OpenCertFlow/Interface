package com.certimakers.diagnosis.application.port.in;

import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 관리자 RAG 품질 검증(F-WADM-015). 임의의 조건으로 근거 검색을 실행해 무엇이 어떤 유사도로
 * 검색되는지 확인한다 — 진단을 돌리지 않고 검색 품질만 점검한다.
 */
public interface VerifyRagUseCase {

    Mono<RagCheckResult> check(RagCheckCommand command);

    record RagCheckCommand(String productGroup, List<String> schemeCodes,
                           List<String> certificationTypes, List<String> sections) {
    }

    /** {@code degraded}가 참이면 RAG 워커 호출이 실패해 근거 없이 진행한 상태다. */
    record RagCheckResult(int count, boolean degraded, List<EvidenceView> evidences) {
    }

    record EvidenceView(String sourceDocumentId, String sectionType, String snippet,
                        String sourceUrl, double relevance) {
    }
}
