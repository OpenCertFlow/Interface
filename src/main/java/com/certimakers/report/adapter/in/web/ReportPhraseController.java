package com.certimakers.report.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.report.application.port.in.ManageReportPhraseUseCase;
import com.certimakers.report.application.port.in.ManageReportPhraseUseCase.PhraseView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** 리포트 문구 공개 조회. 앱·리포트가 관리된 문구를 가져간다(F-WADM-016 소비 측). */
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
