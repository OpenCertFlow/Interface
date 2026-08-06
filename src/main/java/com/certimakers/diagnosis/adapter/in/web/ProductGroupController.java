package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.GetProductGroupSchemaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 제품군·입력 스키마 메타데이터 API (이슈 #6).
 *
 * <p>진단을 시작하기 전에 호출하는 화면 구성용 정보이므로 인증 없이 연다.
 *
 * <p>enum 기본 스키마에 관리자 프레젠테이션 오버라이드(F-WADM-006~008)를 얹은 <b>유효 스키마</b>를
 * 내려보낸다. 오버라이드가 없으면 결과는 enum과 동일하다.
 */
@Tag(name = "제품군·입력 스키마", description = "진단 시작 전 제품군과 입력 항목을 내려받는다(비로그인 가능)")
@WebAdapter
@RequestMapping("/api/v1/product-groups")
public class ProductGroupController {

    private final GetProductGroupSchemaUseCase getProductGroupSchemaUseCase;
    private final TimeProvider timeProvider;

    public ProductGroupController(
            GetProductGroupSchemaUseCase getProductGroupSchemaUseCase, TimeProvider timeProvider) {
        this.getProductGroupSchemaUseCase = getProductGroupSchemaUseCase;
        this.timeProvider = timeProvider;
    }

    @Operation(summary = "제품군 목록·입력 스키마 조회",
            description = "지원 제품군(소형가전·전기방석)과 각 제품군의 입력 항목(유효 스키마)을 준다")
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ProductGroupResponse>>>> productGroups() {
        return getProductGroupSchemaUseCase.getAll()
                .map(schemas -> schemas.stream().map(ProductGroupResponse::from).toList())
                .flatMap(body -> TraceId.current().map(traceId ->
                        ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now()))));
    }
}
