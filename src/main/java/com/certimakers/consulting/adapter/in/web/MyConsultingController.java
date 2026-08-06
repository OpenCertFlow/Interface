package com.certimakers.consulting.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.consulting.application.port.in.GetMyLeadsUseCase;
import com.certimakers.consulting.application.port.in.GetMyLeadsUseCase.MyLeadView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/** 소공인의 내 상담 조회(F-APP-041). /api/v1/me/** 는 인증이 필요하다. */
@Tag(name = "내 상담", description = "내가 신청한 상담 내역")
@WebAdapter
@RequestMapping("/api/v1/me/consulting-leads")
public class MyConsultingController {

    private final GetMyLeadsUseCase getMyLeadsUseCase;
    private final TimeProvider timeProvider;

    public MyConsultingController(GetMyLeadsUseCase getMyLeadsUseCase, TimeProvider timeProvider) {
        this.getMyLeadsUseCase = getMyLeadsUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<MyLeadView>>>> myLeads(
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getName())
                .flatMap(userId -> getMyLeadsUseCase.myLeads(userId, limit))
                .flatMap(body -> TraceId.current().map(traceId ->
                        ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now()))));
    }
}
