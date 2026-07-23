package com.certimakers.diagnosis.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
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
 * <p>유스케이스를 거치지 않고 도메인 enum을 바로 읽는다. 조회할 상태도, 내릴 판단도 없는
 * <b>정적 정의</b>라 유스케이스 계층을 두면 위임만 하는 껍데기가 하나 늘 뿐이다.
 */
@WebAdapter
@RequestMapping("/api/v1/product-groups")
public class ProductGroupController {

    private final TimeProvider timeProvider;

    public ProductGroupController(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ProductGroupResponse>>>> productGroups() {
        return TraceId.current().map(traceId -> ResponseEntity.ok(
                ApiResponse.success(ProductGroupResponse.all(), traceId, timeProvider.now())));
    }
}
