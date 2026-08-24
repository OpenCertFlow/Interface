package io.opencertflow.diagnosis.adapter.in.web.prep;

import io.opencertflow.common.adapter.in.web.annotation.WebAdapter;
import io.opencertflow.common.adapter.in.web.response.ApiResponse;
import io.opencertflow.common.adapter.in.web.trace.TraceId;
import io.opencertflow.common.domain.port.TimeProvider;
import io.opencertflow.diagnosis.application.port.in.ManagePrepPlanUseCase;
import io.opencertflow.diagnosis.domain.model.DiagnosisId;
import io.opencertflow.diagnosis.domain.model.DocumentCode;
import io.opencertflow.diagnosis.domain.model.PrepPlan;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 인증 준비 트래커 API(F-APP-049). 모든 경로가 로그인 사용자 본인 소유로 한정된다
 * ({@code /api/v1/me/**}가 SecurityConfig에서 이미 인증 대상이다).
 *
 * <p>소유자 id를 URL로 받지 않는다 — 토큰에서 꺼내므로 남의 목록을 지정할 방법이 구조적으로 없다.
 * 경로에 쓰는 id는 진단 id다. 준비계획은 진단당 하나뿐이라 별도 id로 접근할 일이 없고, 앱이 진단
 * 화면에서 바로 부를 수 있다(재진단 비교가 재진단 id 하나만 받는 것과 같은 판단).
 */
@Tag(name = "인증 준비 트래커", description = "누락 서류를 확보해 가며 체크하는 목록(본인만)")
@WebAdapter
@RequestMapping("/api/v1/me/prep-plans")
public class PrepPlanController {

    private final ManagePrepPlanUseCase prepPlanUseCase;
    private final TimeProvider timeProvider;

    public PrepPlanController(ManagePrepPlanUseCase prepPlanUseCase, TimeProvider timeProvider) {
        this.prepPlanUseCase = prepPlanUseCase;
        this.timeProvider = timeProvider;
    }

    /**
     * 준비목록을 확보한다 — 없으면 만들고, 이미 있으면 그대로 돌려준다.
     *
     * <p>POST가 아니라 PUT인 이유: 진단당 계획이 하나뿐이라 몇 번을 불러도 결과가 같다(멱등).
     * 앱은 트래커 화면에 들어올 때마다 이것만 부르면 되고, "처음인지"를 기억할 필요가 없다.
     */
    @PutMapping("/{diagnosisId}")
    public Mono<ResponseEntity<ApiResponse<PrepPlanResponse>>> createOrGet(
            @PathVariable Long diagnosisId, Mono<Principal> principal) {
        return userId(principal)
                .flatMap(requester -> prepPlanUseCase.createOrGet(
                        DiagnosisId.of(diagnosisId), requester))
                .flatMap(plan -> wrap(plan, HttpStatus.OK));
    }

    /**
     * 준비 현황 조회. 목록을 아직 만들지 않았으면 404다.
     *
     * <p><b>만들지 않는다.</b> 진단 목록처럼 여러 건을 훑는 화면에서 상태만 확인할 때 쓴다 —
     * 거기서 PUT을 부르면 훑기만 해도 계획이 우수수 생긴다.
     */
    @GetMapping("/{diagnosisId}")
    public Mono<ResponseEntity<ApiResponse<PrepPlanResponse>>> get(
            @PathVariable Long diagnosisId, Mono<Principal> principal) {
        return userId(principal)
                .flatMap(requester -> prepPlanUseCase.get(DiagnosisId.of(diagnosisId), requester))
                .flatMap(plan -> wrap(plan, HttpStatus.OK));
    }

    /** 항목 체크·해제. 목록에 없는 서류 코드는 OCF-DIAG-008로 거부된다. */
    @PatchMapping("/{diagnosisId}/items/{documentCode}")
    public Mono<ResponseEntity<ApiResponse<PrepPlanResponse>>> check(
            @PathVariable Long diagnosisId,
            @PathVariable String documentCode,
            @Valid @RequestBody PrepItemCheckRequest request,
            Mono<Principal> principal) {
        return userId(principal)
                .flatMap(requester -> prepPlanUseCase.check(
                        DiagnosisId.of(diagnosisId),
                        DocumentCode.of(documentCode),
                        request.done(),
                        requester))
                .flatMap(plan -> wrap(plan, HttpStatus.OK));
    }

    /** 인증 주체의 이름이 곧 userId다(JWT subject). */
    private Mono<String> userId(Mono<Principal> principal) {
        return principal.map(Principal::getName);
    }

    private Mono<ResponseEntity<ApiResponse<PrepPlanResponse>>> wrap(
            PrepPlan plan, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(
                        PrepPlanResponse.from(plan), traceId, timeProvider.now())));
    }
}
