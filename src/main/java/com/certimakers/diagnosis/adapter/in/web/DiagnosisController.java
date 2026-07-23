package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.DiagnoseCommand;
import com.certimakers.diagnosis.application.port.in.DiagnoseProductUseCase;
import com.certimakers.diagnosis.application.port.in.ExportReportPdfQuery;
import com.certimakers.diagnosis.application.port.in.GetDiagnosisReportQuery;
import com.certimakers.diagnosis.application.port.in.GetRemediationPlanQuery;
import com.certimakers.diagnosis.application.port.in.SimulateCommand;
import com.certimakers.diagnosis.application.port.in.SimulateDiagnosisUseCase;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/**
 * 진단 API. 요청 검증·변환과 응답 봉투 조립만 하고 비즈니스 판단은 하지 않는다(헥사고날 인바운드 어댑터).
 */
@WebAdapter
@RequestMapping("/api/v1/diagnoses")
public class DiagnosisController {

    private final DiagnoseProductUseCase diagnoseProductUseCase;
    private final GetDiagnosisReportQuery getDiagnosisReportQuery;
    private final SimulateDiagnosisUseCase simulateDiagnosisUseCase;
    private final GetRemediationPlanQuery getRemediationPlanQuery;
    private final ExportReportPdfQuery exportReportPdfQuery;
    private final DiagnosisWebMapper webMapper;
    private final SimulationWebMapper simulationWebMapper;
    private final TimeProvider timeProvider;

    public DiagnosisController(
            DiagnoseProductUseCase diagnoseProductUseCase,
            GetDiagnosisReportQuery getDiagnosisReportQuery,
            SimulateDiagnosisUseCase simulateDiagnosisUseCase,
            GetRemediationPlanQuery getRemediationPlanQuery,
            ExportReportPdfQuery exportReportPdfQuery,
            DiagnosisWebMapper webMapper,
            SimulationWebMapper simulationWebMapper,
            TimeProvider timeProvider) {
        this.diagnoseProductUseCase = diagnoseProductUseCase;
        this.getDiagnosisReportQuery = getDiagnosisReportQuery;
        this.simulateDiagnosisUseCase = simulateDiagnosisUseCase;
        this.getRemediationPlanQuery = getRemediationPlanQuery;
        this.exportReportPdfQuery = exportReportPdfQuery;
        this.webMapper = webMapper;
        this.simulationWebMapper = simulationWebMapper;
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

    /**
     * 반사실 시뮬레이션. "이 서류를 준비하면 / 이 사양을 바꾸면 결과가 어떻게 달라지는가".
     *
     * <p>결과를 저장하지 않으므로 조회 성격이지만, 가정을 본문으로 받아야 해서 POST를 쓴다.
     * 저장하지 않는다는 사실이 응답의 {@code notice}에 명시된다.
     */
    @PostMapping("/{id}/simulations")
    public Mono<ResponseEntity<ApiResponse<SimulationResponse>>> simulate(
            @PathVariable UUID id, @Valid @RequestBody SimulateRequest request) {

        SimulateCommand command = new SimulateCommand(
                DiagnosisId.of(id), simulationWebMapper.toAdjustment(request));

        return simulateDiagnosisUseCase.simulate(command)
                .flatMap(outcome -> TraceId.current().map(traceId -> ResponseEntity.ok(
                        ApiResponse.success(
                                simulationWebMapper.toResponse(id.toString(), outcome),
                                traceId,
                                timeProvider.now()))));
    }

    /** 목표 준비도에 도달하기 위한 최소 보완 경로. */
    @GetMapping("/{id}/remediation-plan")
    public Mono<ResponseEntity<ApiResponse<RemediationPlanResponse>>> remediationPlan(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "100") int targetScore) {

        return getRemediationPlanQuery.plan(DiagnosisId.of(id), targetScore)
                .flatMap(plan -> TraceId.current().map(traceId -> ResponseEntity.ok(
                        ApiResponse.success(
                                simulationWebMapper.toResponse(id.toString(), plan),
                                traceId,
                                timeProvider.now()))));
    }

    /**
     * 진단 리포트 PDF 내려받기. 상담 자리에 그대로 들고 갈 수 있는 형태다.
     *
     * <p>저장하지 않고 매번 다시 그린다 — 표현이 바뀌어도 최신 형식으로 나온다.
     */
    @GetMapping(value = "/{id}/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public Mono<ResponseEntity<byte[]>> exportReportPdf(@PathVariable UUID id) {
        return exportReportPdfQuery.export(id.toString())
                .map(report -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(report.content().length)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                        .filename(report.fileName(), StandardCharsets.UTF_8)
                                        .build().toString())
                        .body(report.content()));
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
