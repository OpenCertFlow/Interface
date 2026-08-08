package io.opencertflow.diagnosis.adapter.in.web.draft;

import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.ManageDiagnosisDraftUseCase;
import io.opencertflow.diagnosis.domain.model.DiagnosisDraft;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 진단 입력 초안 API(F-APP-004). 모든 경로가 로그인 사용자 본인 소유로 한정된다
 * (SecurityConfig가 인증을 강제한다).
 */
@Tag(name = "진단 초안", description = "미완성 입력 저장·조회·수정·삭제(본인만)")
@WebAdapter
@RequestMapping("/api/v1/diagnoses/drafts")
public class DiagnosisDraftController {

    private final ManageDiagnosisDraftUseCase draftUseCase;
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;

    public DiagnosisDraftController(
            ManageDiagnosisDraftUseCase draftUseCase, ObjectMapper objectMapper,
            TimeProvider timeProvider) {
        this.draftUseCase = draftUseCase;
        this.objectMapper = objectMapper;
        this.timeProvider = timeProvider;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<DraftResponse>>> create(
            @Valid @RequestBody DraftRequest request, Mono<Principal> principal) {
        return userId(principal)
                .flatMap(owner -> draftUseCase.create(
                        owner, request.productGroup(), request.input().toString()))
                .flatMap(draft -> wrap(draft, HttpStatus.CREATED));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<DraftResponse>>>> listMine(Mono<Principal> principal) {
        return userId(principal)
                .flatMap(draftUseCase::listMine)
                .flatMap(drafts -> {
                    List<DraftResponse> body = drafts.stream()
                            .map(draft -> DraftResponse.from(draft, objectMapper)).toList();
                    return TraceId.current().map(traceId -> ResponseEntity.ok(
                            ApiResponse.success(body, traceId, timeProvider.now())));
                });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<DraftResponse>>> get(
            @PathVariable long id, Mono<Principal> principal) {
        return userId(principal)
                .flatMap(owner -> draftUseCase.get(id, owner))
                .flatMap(draft -> wrap(draft, HttpStatus.OK));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<DraftResponse>>> update(
            @PathVariable long id, @Valid @RequestBody DraftRequest request,
            Mono<Principal> principal) {
        return userId(principal)
                .flatMap(owner -> draftUseCase.update(
                        id, owner, request.productGroup(), request.input().toString()))
                .flatMap(draft -> wrap(draft, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(
            @PathVariable long id, Mono<Principal> principal) {
        return userId(principal)
                .flatMap(owner -> draftUseCase.delete(id, owner))
                .then(TraceId.current().map(traceId -> ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(ApiResponse.<Void>success(null, traceId, timeProvider.now()))));
    }

    private Mono<String> userId(Mono<Principal> principal) {
        return principal.map(Principal::getName);
    }

    private Mono<ResponseEntity<ApiResponse<DraftResponse>>> wrap(
            DiagnosisDraft draft, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(
                        DraftResponse.from(draft, objectMapper), traceId, timeProvider.now())));
    }
}
