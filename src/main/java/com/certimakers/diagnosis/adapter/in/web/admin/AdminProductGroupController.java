package com.certimakers.diagnosis.adapter.in.web.admin;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.ManageProductGroupQuestionUseCase;
import com.certimakers.diagnosis.application.port.in.ManageProductGroupQuestionUseCase.Option;
import com.certimakers.diagnosis.application.port.in.ManageProductGroupQuestionUseCase.QuestionAdminView;
import com.certimakers.diagnosis.application.port.in.ManageProductGroupQuestionUseCase.UpdateQuestionCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 관리자 제품군 질문(입력 항목) 관리 API(F-WADM-006~008).
 *
 * <p>지금까지 입력 항목은 {@code ProductGroup} enum 코드로만 바꿀 수 있었다. 이 컨트롤러는 그
 * <b>프레젠테이션</b>(라벨·도움말·필수·순서·노출·보기)을 API로 편집한다. 코드·타입·의존은 진단
 * DTO·룰과 묶인 계약이라 편집 대상이 아니며, 없는 코드를 편집하면 400을 낸다.
 *
 * <p>접근 제어는 경로 규칙({@code /api/v1/admin/**} → ADMIN)이 담당한다.
 */
@WebAdapter
@RequestMapping("/api/v1/admin/product-groups")
public class AdminProductGroupController {

    private final ManageProductGroupQuestionUseCase manageQuestionUseCase;
    private final TimeProvider timeProvider;

    public AdminProductGroupController(
            ManageProductGroupQuestionUseCase manageQuestionUseCase, TimeProvider timeProvider) {
        this.manageQuestionUseCase = manageQuestionUseCase;
        this.timeProvider = timeProvider;
    }

    /** 제품군의 입력 항목(숨김 포함)을 편집용으로 조회한다. */
    @GetMapping("/{group}/questions")
    public Mono<ResponseEntity<ApiResponse<List<QuestionAdminView>>>> list(
            @PathVariable String group) {
        return manageQuestionUseCase.list(group).flatMap(body -> wrap(body, HttpStatus.OK));
    }

    /** 항목의 프레젠테이션을 갱신한다. null 필드는 enum 기본값으로 되돌림을 뜻한다. */
    @PutMapping("/{group}/questions/{code}")
    public Mono<ResponseEntity<ApiResponse<Void>>> update(
            @PathVariable String group,
            @PathVariable String code,
            @Valid @RequestBody UpdateQuestionRequest request) {

        List<Option> options = request.options() == null ? null
                : request.options().stream()
                        .map(o -> new Option(o.value(), o.label()))
                        .toList();
        return manageQuestionUseCase.update(group, code, new UpdateQuestionCommand(
                        request.label(), request.helpText(), request.required(),
                        request.displayOrder(), request.active(), options))
                .then(wrap(null, HttpStatus.OK));
    }

    // ── 요청 DTO ─────────────────────────────────────────────────

    /**
     * 프레젠테이션 편집 요청. {@code active}만 필수(기본 노출 여부는 명시), 나머지 null은
     * enum 기본값으로 되돌림을 뜻한다.
     */
    public record UpdateQuestionRequest(
            String label,
            String helpText,
            Boolean required,
            Integer displayOrder,
            boolean active,
            List<OptionRequest> options) {
    }

    public record OptionRequest(String value, String label) {
    }

    private <T> Mono<ResponseEntity<ApiResponse<T>>> wrap(T body, HttpStatus status) {
        return TraceId.current().map(traceId -> ResponseEntity.status(status)
                .body(ApiResponse.success(body, traceId, timeProvider.now())));
    }
}
