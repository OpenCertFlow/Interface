package io.opencertflow.diagnosis.adapter.in.web.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.ManageRuleSetUseCase;
import io.opencertflow.diagnosis.application.port.in.ManageRuleSetUseCase.CreateRuleSetCommand;
import io.opencertflow.diagnosis.application.port.in.ManageRuleSetUseCase.RuleDraft;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 관리자 룰셋 관리 API(F-WADM-009 규칙 관리, F-WADM-010 검증·배포).
 *
 * <p>지금까지 룰은 {@code R__seed_rules*.sql}로만 바꿀 수 있었다. 이 컨트롤러는 그 작업을 API로
 * 옮긴다 — 조회·검증·초안 저장·배포(활성화)를 화면에서 수행한다.
 *
 * <p>접근 제어는 경로 규칙({@code /api/v1/admin/**} → ADMIN)이 담당한다. 컨트롤러는 다시 권한을
 * 확인하지 않는다(판단이 두 곳이면 언젠가 어긋난다).
 */
@Tag(name = "관리자 · 룰셋", description = "룰셋 등록·검증·활성화")
@WebAdapter
@RequestMapping("/api/v1/admin/rule-sets")
public class AdminRuleSetController {

    private final ManageRuleSetUseCase manageRuleSetUseCase;
    private final TimeProvider timeProvider;

    public AdminRuleSetController(
            ManageRuleSetUseCase manageRuleSetUseCase, TimeProvider timeProvider) {
        this.manageRuleSetUseCase = manageRuleSetUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ManageRuleSetUseCase.RuleSetSummary>>>> list() {
        return manageRuleSetUseCase.list().flatMap(body -> wrap(body, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<ManageRuleSetUseCase.RuleSetDetail>>> get(
            @PathVariable String id) {
        return manageRuleSetUseCase.get(Long.parseLong(id))
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    /** 저장하지 않고 룰 정의를 파싱 검증한다. 배포 전 안전 확인용. */
    @PostMapping("/validate")
    public Mono<ResponseEntity<ApiResponse<ManageRuleSetUseCase.ValidationResult>>> validate(
            @Valid @RequestBody ValidateRequest request) {
        return manageRuleSetUseCase.validate(toDrafts(request.rules()))
                .flatMap(body -> wrap(body, HttpStatus.OK));
    }

    /** 새 룰셋 초안(비활성)을 만든다. 검증 실패 시 저장하지 않고 400을 낸다. */
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<CreatedView>>> create(
            @Valid @RequestBody CreateRuleSetRequest request) {
        return manageRuleSetUseCase.createDraft(
                        new CreateRuleSetCommand(request.productGroup(), toDrafts(request.rules())))
                .map(id -> new CreatedView(id.toString()))
                .flatMap(body -> wrap(body, HttpStatus.CREATED));
    }

    /** 룰셋을 활성화(배포)한다. 같은 제품군의 기존 활성본은 자동 비활성화된다. */
    @PostMapping("/{id}/activate")
    public Mono<ResponseEntity<ApiResponse<Void>>> activate(@PathVariable String id) {
        return manageRuleSetUseCase.activate(Long.parseLong(id))
                .then(wrap(null, HttpStatus.OK));
    }

    // ── 요청/응답 DTO ────────────────────────────────────────────

    public record RuleDraftRequest(
            @NotBlank(message = "ruleCode가 필요합니다.") String ruleCode,
            int priority,
            @NotBlank(message = "condition이 필요합니다.") String conditionJson,
            @NotBlank(message = "effects가 필요합니다.") String effectsJson,
            String description) {
    }

    public record ValidateRequest(
            @NotEmpty(message = "룰이 하나 이상 필요합니다.")
            @Valid List<RuleDraftRequest> rules) {
    }

    public record CreateRuleSetRequest(
            @NotBlank(message = "productGroup이 필요합니다.") String productGroup,
            @NotEmpty(message = "룰이 하나 이상 필요합니다.")
            @Valid List<RuleDraftRequest> rules) {
    }

    public record CreatedView(String id) {
    }

    private static List<RuleDraft> toDrafts(List<RuleDraftRequest> rules) {
        return rules.stream()
                .map(r -> new RuleDraft(
                        r.ruleCode(), r.priority(), r.conditionJson(), r.effectsJson(),
                        r.description()))
                .toList();
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
