package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.DiagnoseCommand;
import com.certimakers.diagnosis.application.port.in.DiagnoseProductUseCase;
import com.certimakers.diagnosis.application.port.in.GetDiagnosisReportQuery;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 진단 API. 요청 검증·변환과 응답 봉투 조립만 하고 비즈니스 판단은 하지 않는다(헥사고날 인바운드 어댑터).
 */
@WebAdapter
@RequestMapping("/api/v1/diagnoses")
public class DiagnosisController {

    private final DiagnoseProductUseCase diagnoseProductUseCase;
    private final GetDiagnosisReportQuery getDiagnosisReportQuery;
    private final DiagnosisWebMapper webMapper;
    private final TimeProvider timeProvider;

    public DiagnosisController(
            DiagnoseProductUseCase diagnoseProductUseCase,
            GetDiagnosisReportQuery getDiagnosisReportQuery,
            DiagnosisWebMapper webMapper,
            TimeProvider timeProvider) {
        this.diagnoseProductUseCase = diagnoseProductUseCase;
        this.getDiagnosisReportQuery = getDiagnosisReportQuery;
        this.webMapper = webMapper;
        this.timeProvider = timeProvider;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<DiagnosisReportResponse>>> diagnose(
            @Valid @RequestBody DiagnoseRequest request) {
        DiagnoseCommand command = new DiagnoseCommand(webMapper.toProfile(request));
        return diagnoseProductUseCase.diagnose(command)
                .flatMap(diagnosis -> wrap(diagnosis, HttpStatus.CREATED));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<DiagnosisReportResponse>>> getReport(
            @PathVariable UUID id) {
        return getDiagnosisReportQuery.getById(DiagnosisId.of(id))
                .flatMap(diagnosis -> wrap(diagnosis, HttpStatus.OK));
    }

    private Mono<ResponseEntity<ApiResponse<DiagnosisReportResponse>>> wrap(
            Diagnosis diagnosis, HttpStatus status) {
        return TraceId.current().map(traceId -> {
            DiagnosisReportResponse body = webMapper.toResponse(diagnosis);
            return ResponseEntity.status(status)
                    .body(ApiResponse.success(body, traceId, timeProvider.now()));
        });
    }
}
