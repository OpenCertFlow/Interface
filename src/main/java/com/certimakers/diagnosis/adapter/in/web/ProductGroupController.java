package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.in.GetProductGroupSchemaUseCase;
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

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ProductGroupResponse>>>> productGroups() {
        return getProductGroupSchemaUseCase.getAll()
                .map(schemas -> schemas.stream().map(ProductGroupResponse::from).toList())
                .flatMap(body -> TraceId.current().map(traceId ->
                        ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now()))));
    }
}
