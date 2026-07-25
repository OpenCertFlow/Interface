package com.certimakers.auth.adapter.in.web;

import com.certimakers.auth.application.port.in.GetTermsUseCase;
import com.certimakers.auth.application.port.in.GetTermsUseCase.TermView;
import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** 약관 조회(F-AUTH-008). 회원가입 전 화면 구성용이라 인증 없이 연다. */
@WebAdapter
@RequestMapping("/api/v1/terms")
public class TermsController {

    private final GetTermsUseCase getTermsUseCase;
    private final TimeProvider timeProvider;

    public TermsController(GetTermsUseCase getTermsUseCase, TimeProvider timeProvider) {
        this.getTermsUseCase = getTermsUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<TermView>>>> current() {
        return getTermsUseCase.current()
                .flatMap(body -> TraceId.current().map(traceId ->
                        ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now()))));
    }
}
