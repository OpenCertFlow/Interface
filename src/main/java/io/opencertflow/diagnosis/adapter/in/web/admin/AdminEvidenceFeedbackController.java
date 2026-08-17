package io.opencertflow.diagnosis.adapter.in.web.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.EvidenceFeedbackUseCase;
import io.opencertflow.diagnosis.application.port.in.EvidenceFeedbackUseCase.DocumentFeedbackSummary;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 근거 피드백 집계. 어떤 공식 문서부터 손봐야 하는지 알려 준다.
 *
 * <p>재검토 필요 건수가 많은 문서가 위로 온다. 관리자는 위에서부터 원문을 다시 보고 색인 태그나
 * 청킹을 고치면 된다.
 */
@Tag(name = "관리자 · 근거 피드백", description = "컨설턴트가 남긴 근거 평가 집계")
@WebAdapter
@RequestMapping("/api/v1/admin/evidence-feedback")
public class AdminEvidenceFeedbackController {

    private final EvidenceFeedbackUseCase feedbackUseCase;
    private final TimeProvider timeProvider;

    public AdminEvidenceFeedbackController(
            EvidenceFeedbackUseCase feedbackUseCase, TimeProvider timeProvider) {
        this.feedbackUseCase = feedbackUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<DocumentFeedbackSummary>>>> summary() {
        return feedbackUseCase.summary()
                .flatMap(rows -> TraceId.current().map(traceId -> ResponseEntity.ok(
                        ApiResponse.success(rows, traceId, timeProvider.now()))));
    }
}
