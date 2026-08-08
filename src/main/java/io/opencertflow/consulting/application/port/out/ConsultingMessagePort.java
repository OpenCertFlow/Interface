package io.opencertflow.consulting.application.port.out;

import io.opencertflow.consulting.domain.model.ConsultingMessage;
import java.util.List;

/** 상담 메시지 저장·조회. 블로킹(JPA)이라 호출자는 BlockingBridge로 감싼다. */
public interface ConsultingMessagePort {

    void append(ConsultingMessage message);

    List<ConsultingMessage> findByLead(Long leadId);
}
