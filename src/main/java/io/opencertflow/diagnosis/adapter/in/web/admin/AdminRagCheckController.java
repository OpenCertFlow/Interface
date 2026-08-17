package io.opencertflow.diagnosis.adapter.in.web.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.VerifyRagUseCase;
import io.opencertflow.diagnosis.application.port.in.VerifyRagUseCase.RagCheckCommand;
import io.opencertflow.diagnosis.application.port.in.VerifyRagUseCase.RagCheckResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 관리자 RAG 품질 검증 API(F-WADM-015). 임의 조건으로 근거 검색을 실행해 결과·유사도를 본다.
 * RAG 워커가 꺼져 있으면 degraded=true로 응답한다(예외로 죽지 않음).
 */
@Tag(name = "관리자 · RAG 점검", description = "검색 품질 수동 확인")
@WebAdapter
@RequestMapping("/api/v1/admin/rag-check")
public class AdminRagCheckController {

    private final VerifyRagUseCase verifyRagUseCase;
    private final TimeProvider timeProvider;

    public AdminRagCheckController(VerifyRagUseCase verifyRagUseCase, TimeProvider timeProvider) {
        this.verifyRagUseCase = verifyRagUseCase;
        this.timeProvider = timeProvider;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<RagCheckResult>>> check(
            @Valid @RequestBody RagCheckRequest request) {
        return verifyRagUseCase.check(new RagCheckCommand(
                        request.productGroup(), request.schemeCodes(),
                        request.certificationTypes(), request.sections()))
                .flatMap(body -> TraceId.current().map(traceId ->
                        ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now()))));
    }

    public record RagCheckRequest(
            @NotBlank(message = "productGroup이 필요합니다.") String productGroup,
            List<String> schemeCodes,
            List<String> certificationTypes,
            List<String> sections) {
    }
}
