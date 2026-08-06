package com.certimakers.consulting.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.certimakers.common.adapter.in.web.response.ApiResponse;
import com.certimakers.common.adapter.in.web.trace.TraceId;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.consulting.application.port.in.RequestConsultingCommand;
import com.certimakers.consulting.application.port.in.RequestConsultingUseCase;
import com.certimakers.consulting.domain.model.ConsentRecord;
import com.certimakers.consulting.domain.model.ConsultingLead;
import com.certimakers.consulting.domain.model.ContactInfo;
import com.certimakers.consulting.domain.model.DiagnosisReference;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** 컨설팅 연결 API. "진단 → 이해 → 보완 → 상담"의 마지막 단계(기획서). */
@Tag(name = "상담 신청(공개)", description = "소공인 상담 접수·공개 메시지 조회")
@WebAdapter
@RequestMapping("/api/v1/consulting-leads")
public class ConsultingController {

    private final RequestConsultingUseCase requestConsultingUseCase;
    private final TimeProvider timeProvider;

    public ConsultingController(
            RequestConsultingUseCase requestConsultingUseCase, TimeProvider timeProvider) {
        this.requestConsultingUseCase = requestConsultingUseCase;
        this.timeProvider = timeProvider;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<ConsultingLeadResponse>>> request(
            @Valid @RequestBody RequestConsultingRequest request) {
        // 접수는 비로그인도 가능하다(공개). 로그인 상태면 그 사용자를 소유자로 연결해 알림 수신자로 삼는다.
        return currentUserId()
                .flatMap(ownerId -> requestConsultingUseCase.request(toCommand(request, ownerId.orElse(null))))
                .flatMap(this::wrapCreated);
    }

    private RequestConsultingCommand toCommand(RequestConsultingRequest request, String ownerUserId) {
        return new RequestConsultingCommand(
                DiagnosisReference.of(request.diagnosisId()),
                new ContactInfo(request.contactName(), request.contactPhone(), request.contactEmail()),
                request.message(),
                new ConsentRecord(
                        request.privacyConsent(),
                        request.sensitiveInfoConsent(),
                        request.serviceLimitAcknowledged(),
                        request.consentVersion()),
                ownerUserId);
    }

    private Mono<Optional<String>> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> Optional.of(context.getAuthentication().getName()))
                .defaultIfEmpty(Optional.empty());
    }

    private Mono<ResponseEntity<ApiResponse<ConsultingLeadResponse>>> wrapCreated(ConsultingLead lead) {
        return TraceId.current().map(traceId -> {
            ConsultingLeadResponse body = new ConsultingLeadResponse(
                    lead.id().value().toString(),
                    lead.diagnosis().value().toString(),
                    lead.status().name(),
                    lead.contact().maskedPhone(),
                    lead.contact().maskedEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(body, traceId, timeProvider.now()));
        });
    }
}
