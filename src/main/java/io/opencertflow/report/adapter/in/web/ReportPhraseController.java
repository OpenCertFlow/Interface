package io.opencertflow.report.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.report.application.port.in.ManageReportPhraseUseCase;
import io.opencertflow.report.application.port.in.ManageReportPhraseUseCase.PhraseView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** 리포트 문구 공개 조회. 앱·리포트가 관리된 문구를 가져간다(F-WADM-016 소비 측). */
@Tag(name = "리포트 문구", description = "결과 화면 문구 조회(비로그인 가능)")
@WebAdapter
@RequestMapping("/api/v1/report-phrases")
public class ReportPhraseController {

    private final ManageReportPhraseUseCase manageReportPhraseUseCase;
    private final TimeProvider timeProvider;

    public ReportPhraseController(
            ManageReportPhraseUseCase manageReportPhraseUseCase, TimeProvider timeProvider) {
        this.manageReportPhraseUseCase = manageReportPhraseUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<PhraseView>>>> list() {
        return manageReportPhraseUseCase.list()
                .flatMap(body -> TraceId.current().map(traceId ->
                        ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now()))));
    }
}
