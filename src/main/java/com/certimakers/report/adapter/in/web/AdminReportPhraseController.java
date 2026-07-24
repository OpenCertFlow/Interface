package com.certimakers.report.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.report.application.port.in.ManageReportPhraseUseCase;
import com.certimakers.report.application.port.in.ManageReportPhraseUseCase.PhraseView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** 관리자 리포트 문구 관리 API(F-WADM-016). 코드 배포 없이 안내·면책 문구를 편집한다. */
@WebAdapter
@RequestMapping("/api/v1/admin/report-phrases")
public class AdminReportPhraseController {

    private final ManageReportPhraseUseCase manageReportPhraseUseCase;
    private final TimeProvider timeProvider;

    public AdminReportPhraseController(
            ManageReportPhraseUseCase manageReportPhraseUseCase, TimeProvider timeProvider) {
        this.manageReportPhraseUseCase = manageReportPhraseUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<PhraseView>>>> list() {
        return manageReportPhraseUseCase.list().flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PutMapping("/{key}")
    public Mono<ResponseEntity<ApiResponse<Void>>> update(
            @PathVariable String key, @Valid @RequestBody UpdatePhraseRequest request) {
        return manageReportPhraseUseCase.update(key, request.text(), request.description())
                .then(wrap(null, HttpStatus.OK));
    }

    public record UpdatePhraseRequest(
            @NotBlank(message = "문구 내용이 필요합니다.") String text,
            String description) {
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
