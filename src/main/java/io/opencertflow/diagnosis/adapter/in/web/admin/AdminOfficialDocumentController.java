package io.opencertflow.diagnosis.adapter.in.web.admin;

import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.ManageOfficialDocumentUseCase;
import io.opencertflow.diagnosis.application.port.in.ManageOfficialDocumentUseCase.DocumentCommand;
import io.opencertflow.diagnosis.application.port.in.ManageOfficialDocumentUseCase.DocumentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** 관리자 공식 문서 메타데이터 API(F-WADM-012/013). 접근 제어는 경로 규칙(/api/v1/admin/**)이 담당한다. */
@WebAdapter
@RequestMapping("/api/v1/admin/official-documents")
public class AdminOfficialDocumentController {

    private final ManageOfficialDocumentUseCase manageOfficialDocumentUseCase;
    private final TimeProvider timeProvider;

    public AdminOfficialDocumentController(
            ManageOfficialDocumentUseCase manageOfficialDocumentUseCase, TimeProvider timeProvider) {
        this.manageOfficialDocumentUseCase = manageOfficialDocumentUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<DocumentView>>>> list() {
        return manageOfficialDocumentUseCase.list().flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<DocumentView>>> get(@PathVariable String id) {
        return manageOfficialDocumentUseCase.get(Long.parseLong(id))
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<CreatedView>>> register(
            @Valid @RequestBody DocumentRequest request) {
        return manageOfficialDocumentUseCase.register(request.toCommand())
                .map(id -> new CreatedView(id.toString()))
                .flatMap(body -> wrap(body, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> update(
            @PathVariable String id, @Valid @RequestBody DocumentRequest request) {
        return manageOfficialDocumentUseCase.update(Long.parseLong(id), request.toCommand())
                .then(wrap(null, HttpStatus.OK));
    }

    public record DocumentRequest(
            @NotBlank(message = "제목은 필수입니다.") String title,
            @NotBlank(message = "발행 기관은 필수입니다.") String issuer,
            LocalDate publishedAt,
            LocalDate verifiedAt,
            @NotBlank(message = "제품군은 필수입니다.") String productGroup,
            String certificationType,
            String schemeName,
            @NotBlank(message = "출처 URL은 필수입니다.") String sourceUrl) {

        DocumentCommand toCommand() {
            return new DocumentCommand(
                    title, issuer, publishedAt, verifiedAt, productGroup, certificationType,
                    schemeName, sourceUrl);
        }
    }

    public record CreatedView(String id) {
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
