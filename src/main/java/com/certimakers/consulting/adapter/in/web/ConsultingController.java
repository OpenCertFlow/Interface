package com.certimakers.consulting.adapter.in.web;

import com.certimakers.common.adapter.in.web.annotation.WebAdapter;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** 컨설팅 연결 API. "진단 → 이해 → 보완 → 상담"의 마지막 단계(기획서). */
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
        RequestConsultingCommand command = toCommand(request);
        return requestConsultingUseCase.request(command)
                .flatMap(this::wrapCreated);
    }

    private RequestConsultingCommand toCommand(RequestConsultingRequest request) {
        return new RequestConsultingCommand(
                DiagnosisReference.of(request.diagnosisId()),
                new ContactInfo(request.contactName(), request.contactPhone(), request.contactEmail()),
                request.message(),
                new ConsentRecord(
                        request.privacyConsent(),
                        request.sensitiveInfoConsent(),
                        request.serviceLimitAcknowledged(),
                        request.consentVersion()));
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
