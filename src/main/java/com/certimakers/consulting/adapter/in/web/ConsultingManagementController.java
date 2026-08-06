package com.certimakers.consulting.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.consulting.application.port.in.ConsultingMessageUseCase;
import com.certimakers.consulting.application.port.in.ExportBriefingPdfQuery;
import com.certimakers.consulting.application.port.in.ConsultingMessageUseCase.MessageView;
import com.certimakers.consulting.application.port.in.ManageConsultingUseCase;
import com.certimakers.consulting.application.port.in.ManageConsultingUseCase.LeadDetail;
import com.certimakers.consulting.application.port.in.ManageConsultingUseCase.LeadSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/**
 * 컨설턴트 상담 처리 API(F-WCON). CONSULTANT·ADMIN만 접근한다(경로 규칙).
 *
 * <p>배정은 요청자(현재 로그인한 컨설턴트)를 담당으로 지정한다 — 담당 id는 본문이 아니라 인증
 * 주체에서 읽는다.
 */
@Tag(name = "상담 처리(컨설턴트)", description = "상담 배정·상태·메모·추가정보 요청")
@WebAdapter
@RequestMapping("/api/v1/consulting/leads")
public class ConsultingManagementController {

    private final ManageConsultingUseCase manageConsultingUseCase;
    private final ConsultingMessageUseCase consultingMessageUseCase;
    private final ExportBriefingPdfQuery exportBriefingPdfQuery;
    private final TimeProvider timeProvider;

    public ConsultingManagementController(
            ManageConsultingUseCase manageConsultingUseCase,
            ConsultingMessageUseCase consultingMessageUseCase,
            ExportBriefingPdfQuery exportBriefingPdfQuery,
            TimeProvider timeProvider) {
        this.manageConsultingUseCase = manageConsultingUseCase;
        this.consultingMessageUseCase = consultingMessageUseCase;
        this.exportBriefingPdfQuery = exportBriefingPdfQuery;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<LeadSummary>>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return manageConsultingUseCase.list(status, limit).flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<LeadDetail>>> get(@PathVariable String id) {
        return manageConsultingUseCase.get(id).flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PostMapping("/{id}/assign")
    public Mono<ResponseEntity<ApiResponse<LeadDetail>>> assignToMe(@PathVariable String id) {
        return currentUserId()
                .flatMap(consultantId -> manageConsultingUseCase.assign(id, consultantId))
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PostMapping("/{id}/status")
    public Mono<ResponseEntity<ApiResponse<LeadDetail>>> changeStatus(
            @PathVariable String id, @Valid @RequestBody StatusRequest request) {
        return manageConsultingUseCase.changeStatus(id, request.status())
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PutMapping("/{id}/memo")
    public Mono<ResponseEntity<ApiResponse<LeadDetail>>> updateMemo(
            @PathVariable String id, @RequestBody MemoRequest request) {
        return manageConsultingUseCase.updateMemo(id, request.memo())
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @GetMapping("/{id}/messages")
    public Mono<ResponseEntity<ApiResponse<List<MessageView>>>> messages(@PathVariable String id) {
        return consultingMessageUseCase.list(id).flatMap(body -> wrap(body, HttpStatus.OK));
    }

    /** 추가정보 요청(INFO_REQUEST)·공개 안내(REPLY)·내부 메모(NOTE)를 남긴다. */
    @PostMapping("/{id}/messages")
    public Mono<ResponseEntity<ApiResponse<List<MessageView>>>> postMessage(
            @PathVariable String id, @Valid @RequestBody MessageRequest request) {
        return currentUserId()
                .flatMap(authorId ->
                        consultingMessageUseCase.post(id, authorId, request.kind(), request.body()))
                .then(consultingMessageUseCase.list(id))
                .flatMap(body -> wrap(body, HttpStatus.CREATED));
    }

    public record StatusRequest(@NotBlank(message = "상태 값이 필요합니다.") String status) {
    }

    public record MemoRequest(String memo) {
    }

    public record MessageRequest(
            @NotBlank(message = "메시지 종류가 필요합니다.") String kind,
            @NotBlank(message = "메시지 내용이 필요합니다.") String body) {
    }

    /**
     * 상담 준비 브리핑 PDF(F-WCON-012).
     *
     * <p>진단 리포트 PDF는 소공인용이고 이것은 컨설턴트용이다. "무엇이 부족한가 → 왜 그렇게
     * 판정됐는가 → 무엇을 물어야 하는가" 순으로 담아 상담 첫 5분을 줄이는 것이 목적이다.
     */
    @GetMapping(value = "/{id}/briefing.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public Mono<ResponseEntity<byte[]>> briefingPdf(@PathVariable String id) {
        return exportBriefingPdfQuery.render(id).map(bytes -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("상담브리핑-" + id + ".pdf", StandardCharsets.UTF_8)
                                .build().toString())
                .body(bytes));
    }

    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getName());
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
