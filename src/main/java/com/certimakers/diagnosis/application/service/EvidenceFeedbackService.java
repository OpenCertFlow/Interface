package com.certimakers.diagnosis.application.service;

import com.certimakers.common.application.annotation.UseCase;
import com.certimakers.common.application.support.BlockingBridge;
import com.certimakers.common.domain.error.BusinessException;
import com.certimakers.diagnosis.application.port.in.EvidenceFeedbackUseCase;
import com.certimakers.diagnosis.application.port.out.EvidenceFeedbackPort;
import com.certimakers.diagnosis.domain.model.EvidenceVerdict;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * 컨설턴트의 근거 피드백을 받아 색인 재검토 우선순위로 바꾼다.
 *
 * <p>판단 값은 열거형으로 좁힌다. 자유 문자열을 받으면 집계가 불가능해지고, 집계할 수 없는
 * 피드백은 쌓이기만 할 뿐 아무것도 고치지 못한다. 상세한 사정은 {@code comment}에 적는다.
 */
@UseCase
public class EvidenceFeedbackService implements EvidenceFeedbackUseCase {

    private final EvidenceFeedbackPort feedbackPort;
    private final BlockingBridge blockingBridge;

    public EvidenceFeedbackService(
            EvidenceFeedbackPort feedbackPort, BlockingBridge blockingBridge) {
        this.feedbackPort = feedbackPort;
        this.blockingBridge = blockingBridge;
    }

    @Override
    public Mono<Void> report(ReportCommand command) {
        EvidenceVerdict verdict = parseVerdict(command.verdict());
        if (command.sourceDocumentId() == null || command.sourceDocumentId().isBlank()) {
            return Mono.error(BusinessException.invalid("근거 문서 식별자가 필요합니다."));
        }
        return blockingBridge.mono(() -> {
            feedbackPort.save(new EvidenceFeedbackPort.FeedbackData(
                    command.diagnosisId(), command.sourceDocumentId(), command.sectionType(),
                    verdict.name(), command.comment(), command.reportedBy()));
            return true;
        }).then();
    }

    @Override
    public Mono<List<DocumentFeedbackSummary>> summary() {
        return blockingBridge.mono(() -> feedbackPort.summarize().stream()
                .map(row -> new DocumentFeedbackSummary(
                        row.sourceDocumentId(), row.total(), row.usefulCount(),
                        row.needsReviewCount(), row.lastReportedAt()))
                .toList());
    }

    private EvidenceVerdict parseVerdict(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.invalid("판단 값이 필요합니다.");
        }
        try {
            return EvidenceVerdict.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid(
                    "알 수 없는 판단 값입니다: " + raw + ". 가능한 값: USEFUL, IRRELEVANT, OUTDATED, WRONG_PRODUCT");
        }
    }
}
