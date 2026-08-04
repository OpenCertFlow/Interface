package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.MonitorDocumentFreshnessUseCase;
import com.certimakers.diagnosis.application.port.out.FetchDocumentContentPort;
import com.certimakers.diagnosis.application.port.out.OfficialDocumentAdminPort;
import com.certimakers.diagnosis.application.port.out.OfficialDocumentAdminPort.DocumentRow;
import com.certimakers.diagnosis.domain.service.ContentFingerprint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * 공식 문서 원문 변경 감지.
 *
 * <p>흐름은 단순하다. 등록된 문서의 원문을 가져와 지문을 만들고, 저장된 지문과 다르면 변경으로
 * 표시한다. <b>고치지는 않는다</b> — 무엇이 어떻게 달라졌고 우리 룰과 색인을 어떻게 손봐야 하는지는
 * 사람이 원문을 읽고 정할 일이다(운영지침 §9.2).
 *
 * <p>가져오지 못한 문서는 '변경됨'이 아니라 '확인하지 못함'이다. 이 둘을 뭉개면 사이트 점검 한 번에
 * 전체 문서가 재검토 큐로 쏟아지고, 그러면 큐 자체가 무의미해진다.
 */
@UseCase
public class DocumentFreshnessService implements MonitorDocumentFreshnessUseCase {

    private static final Logger log = LoggerFactory.getLogger(DocumentFreshnessService.class);

    private final OfficialDocumentAdminPort documentPort;
    private final FetchDocumentContentPort fetchPort;
    private final BlockingBridge blockingBridge;
    private final TimeProvider timeProvider;

    public DocumentFreshnessService(
            OfficialDocumentAdminPort documentPort,
            FetchDocumentContentPort fetchPort,
            BlockingBridge blockingBridge,
            TimeProvider timeProvider) {
        this.documentPort = documentPort;
        this.fetchPort = fetchPort;
        this.blockingBridge = blockingBridge;
        this.timeProvider = timeProvider;
    }

    @Override
    public Mono<CheckSummary> checkAll() {
        return blockingBridge.mono(() -> {
            List<DocumentRow> documents = documentPort.findAll();
            Instant now = timeProvider.now();
            int checked = 0;
            int changed = 0;
            int unreachable = 0;

            for (DocumentRow document : documents) {
                Optional<String> content = fetchPort.fetch(document.sourceUrl());
                if (content.isEmpty()) {
                    unreachable++;
                    continue;
                }
                String hash = ContentFingerprint.of(content.get());
                if (hash == null) {
                    unreachable++;
                    continue;
                }
                checked++;
                // 첫 확인이면 기준선을 세울 뿐 변경이 아니다. 비교 대상이 없기 때문이다.
                boolean firstTime = document.contentCheckedAt() == null;
                documentPort.recordContentCheck(document.id(), hash, now);
                if (!firstTime && wasChanged(document.id())) {
                    changed++;
                    log.info("공식 문서 원문 변경 감지 — 재검토 필요. id={}, title={}, url={}",
                            document.id(), document.title(), document.sourceUrl());
                }
            }
            log.info("공식 문서 신선도 확인 완료 — 확인 {}건, 변경 {}건, 접근 실패 {}건",
                    checked, changed, unreachable);
            return new CheckSummary(checked, changed, unreachable);
        });
    }

    /** 방금 기록한 결과가 변경으로 남았는지 되읽는다. 판단 주체는 엔티티다. */
    private boolean wasChanged(Long documentId) {
        return documentPort.findById(documentId)
                .map(row -> row.changeDetectedAt() != null)
                .orElse(false);
    }

    @Override
    public Mono<List<StaleDocumentView>> pendingReview() {
        return blockingBridge.mono(() -> documentPort.findChangeDetected().stream()
                .map(row -> new StaleDocumentView(
                        row.id(), row.title(), row.issuer(), row.sourceUrl(),
                        row.contentCheckedAt(), row.changeDetectedAt()))
                .toList());
    }

    @Override
    public Mono<Void> markReviewed(Long documentId) {
        return blockingBridge.mono(() -> documentPort.clearChangeFlag(documentId))
                .flatMap(cleared -> Boolean.TRUE.equals(cleared)
                        ? Mono.empty()
                        : Mono.error(BusinessException.invalid(
                                "문서를 찾을 수 없습니다: " + documentId)));
    }
}
