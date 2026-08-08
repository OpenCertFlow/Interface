package io.opencertflow.document.adapter.in.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.document.application.port.in.DocumentUseCase;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/**
 * 문서 발급 API.
 *
 * <p>양식 목록은 열려 있고(무엇을 만들 수 있는지 보여 주는 정보), 발급과 이력 조회는 인증이 필요하다 —
 * 발급 문서에는 사업자등록번호·연락처가 담긴다.
 */
@WebAdapter
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentUseCase documentUseCase;
    private final TimeProvider timeProvider;

    public DocumentController(DocumentUseCase documentUseCase, TimeProvider timeProvider) {
        this.documentUseCase = documentUseCase;
        this.timeProvider = timeProvider;
    }

    /** 발급 가능한 양식과 입력 항목. 클라이언트가 입력 화면을 서버 정의대로 그린다. */
    @GetMapping("/templates")
    public Mono<ResponseEntity<ApiResponse<List<DocumentResponses.TemplateView>>>> templates() {
        List<DocumentResponses.TemplateView> body = documentUseCase.templates().stream()
                .map(DocumentResponses.TemplateView::from)
                .toList();
        return wrap(body, HttpStatus.OK);
    }

    @PostMapping("/issues")
    public Mono<ResponseEntity<ApiResponse<DocumentResponses.Issued>>> issue(
            @Valid @RequestBody DocumentRequests.Issue request,
            @Parameter(hidden = true) Mono<Principal> principal) {

        return principal.map(Principal::getName)
                .flatMap(issuerId -> documentUseCase.issue(new DocumentUseCase.IssueCommand(
                        request.templateCode(), request.values(), issuerId)))
                .map(DocumentResponses.Issued::from)
                .flatMap(body -> wrap(body, HttpStatus.CREATED));
    }

    @GetMapping("/issues")
    public Mono<ResponseEntity<ApiResponse<List<DocumentResponses.IssuedSummary>>>> myDocuments(
            @Parameter(hidden = true) Mono<Principal> principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return principal.map(Principal::getName)
                .flatMap(userId -> documentUseCase.myDocuments(userId, page, size))
                .map(documents -> documents.stream()
                        .map(DocumentResponses.IssuedSummary::from)
                        .toList())
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @GetMapping("/issues/{documentId}")
    public Mono<ResponseEntity<ApiResponse<DocumentResponses.IssuedDetail>>> get(
            @PathVariable String documentId,
            @Parameter(hidden = true) Mono<Authentication> authentication) {

        return authentication
                .flatMap(auth -> documentUseCase.get(documentId, auth.getName(), hasAdminRole(auth)))
                .map(DocumentResponses.IssuedDetail::from)
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
