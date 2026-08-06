package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.CompareDiagnosisQuery;
import com.certimakers.diagnosis.application.port.in.DiagnoseCommand;
import com.certimakers.diagnosis.application.port.in.DiagnoseProductUseCase;
import com.certimakers.diagnosis.application.port.in.DiagnosisHistoryUseCase;
import com.certimakers.diagnosis.application.port.in.ExportReportPdfQuery;
import com.certimakers.diagnosis.application.port.in.GetDiagnosisInputQuery;
import com.certimakers.diagnosis.application.port.in.GetDiagnosisReportQuery;
import com.certimakers.diagnosis.application.port.in.GetRemediationPlanQuery;
import com.certimakers.diagnosis.application.port.in.SimulateCommand;
import com.certimakers.diagnosis.application.port.in.SimulateDiagnosisUseCase;
import com.certimakers.diagnosis.domain.model.ProductProfile;
import com.certimakers.diagnosis.domain.model.Diagnosis;
import com.certimakers.diagnosis.domain.model.DiagnosisId;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(name = "진단", description = "제품 입력→진단 실행·조회·재진단·비교·이력·삭제")
@WebAdapter
@RequestMapping("/api/v1/diagnoses")
public class DiagnosisController {

    private final DiagnoseProductUseCase diagnoseProductUseCase;
    private final GetDiagnosisReportQuery getDiagnosisReportQuery;
    private final DiagnosisHistoryUseCase diagnosisHistoryUseCase;
    private final SimulateDiagnosisUseCase simulateDiagnosisUseCase;
    private final GetRemediationPlanQuery getRemediationPlanQuery;
    private final ExportReportPdfQuery exportReportPdfQuery;
    private final CompareDiagnosisQuery compareDiagnosisQuery;
    private final GetDiagnosisInputQuery getDiagnosisInputQuery;
    private final DiagnosisWebMapper webMapper;
    private final SimulationWebMapper simulationWebMapper;
    private final TimeProvider timeProvider;

    public DiagnosisController(
            DiagnoseProductUseCase diagnoseProductUseCase,
            GetDiagnosisReportQuery getDiagnosisReportQuery,
            DiagnosisHistoryUseCase diagnosisHistoryUseCase,
            SimulateDiagnosisUseCase simulateDiagnosisUseCase,
            GetRemediationPlanQuery getRemediationPlanQuery,
            ExportReportPdfQuery exportReportPdfQuery,
            CompareDiagnosisQuery compareDiagnosisQuery,
            GetDiagnosisInputQuery getDiagnosisInputQuery,
            DiagnosisWebMapper webMapper,
            SimulationWebMapper simulationWebMapper,
            TimeProvider timeProvider) {
        this.diagnoseProductUseCase = diagnoseProductUseCase;
        this.getDiagnosisReportQuery = getDiagnosisReportQuery;
        this.diagnosisHistoryUseCase = diagnosisHistoryUseCase;
        this.simulateDiagnosisUseCase = simulateDiagnosisUseCase;
        this.getRemediationPlanQuery = getRemediationPlanQuery;
        this.exportReportPdfQuery = exportReportPdfQuery;
        this.compareDiagnosisQuery = compareDiagnosisQuery;
        this.getDiagnosisInputQuery = getDiagnosisInputQuery;
        this.webMapper = webMapper;
        this.simulationWebMapper = simulationWebMapper;
        this.timeProvider = timeProvider;
    }

    /**
     * 진단 실행. 인증은 <b>선택</b>이다 — 비로그인도 진단할 수 있고, 로그인 상태면 소유자가 연결되어
     * '내 진단 이력'(F-APP-032~035)에 남는다.
     */
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<DiagnosisReportResponse>>> diagnose(
            @Valid @RequestBody DiagnoseRequest request, Mono<Authentication> authentication) {
        ProductProfile profile = webMapper.toProfile(request);
        return authentication
                .filter(auth -> auth.isAuthenticated()
                        && !(auth instanceof AnonymousAuthenticationToken))
                .map(auth -> DiagnoseCommand.of(profile, auth.getName()))
                .defaultIfEmpty(DiagnoseCommand.anonymous(profile))
                .flatMap(diagnoseProductUseCase::diagnose)
                .flatMap(diagnosis -> wrap(diagnosis, HttpStatus.CREATED));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<DiagnosisReportResponse>>> getReport(
            @PathVariable Long id) {
        return getDiagnosisReportQuery.getById(DiagnosisId.of(id))
                .flatMap(diagnosis -> wrap(diagnosis, HttpStatus.OK));
    }

    /** 내 진단 이력 목록(F-APP-032). 로그인 사용자 본인의 진단만 최신순으로. */
    @GetMapping("/mine")
    public Mono<ResponseEntity<ApiResponse<java.util.List<DiagnosisSummaryResponse>>>> listMine(
            Mono<Principal> principal) {
        return currentUserId(principal)
                .flatMap(diagnosisHistoryUseCase::listMine)
                .flatMap(summaries -> {
                    var body = summaries.stream().map(DiagnosisSummaryResponse::from).toList();
                    return TraceId.current().map(traceId -> ResponseEntity.ok(
                            ApiResponse.success(body, traceId, timeProvider.now())));
                });
    }

    /**
     * 재진단(F-APP-034). 본인 소유 진단만.
     *
     * <p>앱은 {@code GET /{id}/input}으로 이전 입력을 받아 폼을 채운 뒤 그 내용을 그대로
     * 제출한다. 사용자가 고치지 않았으면 원 진단과 같은 입력이 오므로 결과도 같다.
     */
    @PostMapping("/{id}/rediagnose")
    public Mono<ResponseEntity<ApiResponse<DiagnosisReportResponse>>> rediagnose(
            @PathVariable Long id,
            @Valid @RequestBody DiagnoseRequest request,
            Mono<Principal> principal) {
        ProductProfile updated = webMapper.toProfile(request);
        return currentUserId(principal)
                .flatMap(userId ->
                        diagnosisHistoryUseCase.rediagnose(DiagnosisId.of(id), userId, updated))
                .flatMap(diagnosis -> wrap(diagnosis, HttpStatus.CREATED));
    }

    /** 재진단 화면 프리필용 이전 입력 조회(F-APP-034). 본인 소유만. */
    @GetMapping("/{id}/input")
    public Mono<ResponseEntity<ApiResponse<DiagnoseRequest>>> getInput(
            @PathVariable Long id, Mono<Principal> principal) {
        return currentUserId(principal)
                .flatMap(userId -> getDiagnosisInputQuery.getInput(DiagnosisId.of(id), userId))
                .map(webMapper::toRequest)
                .flatMap(body -> TraceId.current().map(traceId -> ResponseEntity.ok(
                        ApiResponse.success(body, traceId, timeProvider.now()))));
    }

    /** 재진단 결과 비교(F-APP-048). 원 진단은 서버가 previous_id로 찾는다(본인 소유만). */
    @GetMapping("/{id}/compare")
    public Mono<ResponseEntity<ApiResponse<DiagnosisComparisonResponse>>> compare(
            @PathVariable Long id, Mono<Principal> principal) {
        return currentUserId(principal)
                .flatMap(userId -> compareDiagnosisQuery.compare(DiagnosisId.of(id), userId))
                .map(DiagnosisComparisonResponse::from)
                .flatMap(body -> TraceId.current().map(traceId -> ResponseEntity.ok(
                        ApiResponse.success(body, traceId, timeProvider.now()))));
    }

    /** 진단 삭제(F-APP-035). 본인 소유 진단만 지운다. */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(
            @PathVariable Long id, Mono<Principal> principal) {
        return currentUserId(principal)
                .flatMap(userId -> diagnosisHistoryUseCase.delete(DiagnosisId.of(id), userId))
                .then(TraceId.current().map(traceId -> ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ApiResponse.<Void>success(null, traceId, timeProvider.now()))));
    }

    /** 인증 주체의 이름이 곧 userId다(JWT subject). 이 경로들은 SecurityConfig가 인증을 강제하므로 항상 존재한다. */
    private Mono<String> currentUserId(Mono<Principal> principal) {
        return principal.map(Principal::getName);
    }

    /**
     * 반사실 시뮬레이션. "이 서류를 준비하면 / 이 사양을 바꾸면 결과가 어떻게 달라지는가".
     *
     * <p>결과를 저장하지 않으므로 조회 성격이지만, 가정을 본문으로 받아야 해서 POST를 쓴다.
     * 저장하지 않는다는 사실이 응답의 {@code notice}에 명시된다.
     */
    @PostMapping("/{id}/simulations")
    public Mono<ResponseEntity<ApiResponse<SimulationResponse>>> simulate(
            @PathVariable Long id, @Valid @RequestBody SimulateRequest request) {

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
            @PathVariable Long id,
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
    public Mono<ResponseEntity<byte[]>> exportReportPdf(@PathVariable Long id) {
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
