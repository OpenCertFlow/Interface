package io.opencertflow.consulting.application.port.in;

import io.opencertflow.consulting.domain.model.ConsultingLead;
import reactor.core.publisher.Mono;

/** 인바운드 포트: 진단 결과에 연결된 상담 요청을 접수한다. */
public interface RequestConsultingUseCase {

    Mono<ConsultingLead> request(RequestConsultingCommand command);
}
