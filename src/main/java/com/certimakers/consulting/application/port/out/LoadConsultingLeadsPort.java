package com.certimakers.consulting.application.port.out;

import com.certimakers.consulting.domain.model.ConsultingLead;
import com.certimakers.consulting.domain.model.ConsultingLeadId;
import java.util.List;
import java.util.Optional;

/** 컨설팅 리드 조회(컨설턴트 워크플로). 블로킹(JPA)이라 호출자는 BlockingBridge로 감싼다. */
public interface LoadConsultingLeadsPort {

    List<ConsultingLead> findLeads(String statusFilter, int limit);

    Optional<ConsultingLead> findLead(ConsultingLeadId id);
}
