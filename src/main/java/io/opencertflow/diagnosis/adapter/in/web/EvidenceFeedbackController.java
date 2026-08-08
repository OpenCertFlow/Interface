package io.opencertflow.diagnosis.adapter.in.web;

import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.EvidenceFeedbackUseCase;
import io.opencertflow.diagnosis.application.port.in.EvidenceFeedbackUseCase.ReportCommand;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 컨설턴트가 상담 중에 근거의 적절성을 되먹인다(F-WCON 연장).
 *
 * <p>그 근거가 이 제품에 맞는지 가장 잘 아는 사람은 상담을 처리하는 컨설턴트다. 판단을 받아 두면
 * 색인 재검토의 우선순위가 생긴다.
 */
@WebAdapter
@RequestMapping("/api/v1/consulting/diagnoses/{diagnosisId}/evidence-feedback")
public class EvidenceFeedbackController {

    private final EvidenceFeedbackUseCase feedbackUseCase;
    private final TimeProvider timeProvider;

    public EvidenceFeedbackController(
            EvidenceFeedbackUseCase feedbackUseCase, TimeProvider timeProvider) {
        this.feedbackUseCase = feedbackUseCase;
        this.timeProvider = timeProvider;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Void>>> report(
            @PathVariable String diagnosisId,
            @RequestBody FeedbackRequest request) {

        return currentUserId()
                .flatMap(userId -> feedbackUseCase.report(new ReportCommand(
                        Long.parseLong(diagnosisId),
                        request.sourceDocumentId(),
                        request.sectionType(),
                        request.verdict(),
                        request.comment(),
                        userId)))
                .then(TraceId.current().map(traceId -> ResponseEntity.ok(
                        ApiResponse.<Void>success(null, traceId, timeProvider.now()))));
    }

    /**
     * @param verdict USEFUL · IRRELEVANT · OUTDATED · WRONG_PRODUCT.
     *                자유 문자열을 받지 않는 이유는 집계할 수 없는 피드백은 아무것도 고치지
     *                못하기 때문이다. 사정은 comment에 적는다
     */
    public record FeedbackRequest(
            @NotBlank String sourceDocumentId,
            String sectionType,
            @NotBlank String verdict,
            String comment) {
    }

    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getName());
    }
}
