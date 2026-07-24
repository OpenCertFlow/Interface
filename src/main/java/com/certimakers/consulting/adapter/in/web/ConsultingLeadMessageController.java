package com.certimakers.consulting.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.consulting.application.port.in.ConsultingMessageUseCase;
import com.certimakers.consulting.application.port.in.ConsultingMessageUseCase.MessageView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * 소공인의 상담 공개 메시지 조회(F-WCON-009 소비 측). 리드 id(UUID)를 접근키로 쓴다 — 공개
 * 메시지(추가정보 요청·안내)만 보이고 내부 메모·작성자는 제외한다. 접수 경로와 같은 공개 path다.
 */
@WebAdapter
@RequestMapping("/api/v1/consulting-leads/{leadId}/messages")
public class ConsultingLeadMessageController {

    private final ConsultingMessageUseCase consultingMessageUseCase;
    private final TimeProvider timeProvider;

    public ConsultingLeadMessageController(
            ConsultingMessageUseCase consultingMessageUseCase, TimeProvider timeProvider) {
        this.consultingMessageUseCase = consultingMessageUseCase;
        this.timeProvider = timeProvider;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<MessageView>>>> publicMessages(
            @PathVariable String leadId) {
        return consultingMessageUseCase.listPublic(leadId)
                .flatMap(body -> TraceId.current().map(traceId ->
                        ResponseEntity.ok(ApiResponse.success(body, traceId, timeProvider.now()))));
    }
}
