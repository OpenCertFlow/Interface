package com.certimakers.consulting.adapter.out.persistence;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.consulting.application.port.out.SaveConsultingLeadPort;
import com.certimakers.consulting.domain.model.ConsultingLead;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SaveConsultingLeadPort} 구현. 리드와 동의 로그를 한 트랜잭션에 함께 저장한다.
 * 연락처 암호화는 매퍼가 수행한다.
 */
@PersistenceAdapter
public class ConsultingLeadPersistenceAdapter implements SaveConsultingLeadPort {

    private final ConsultingLeadJpaRepository repository;
    private final ConsultingLeadMapper mapper;

    public ConsultingLeadPersistenceAdapter(
            ConsultingLeadJpaRepository repository, ConsultingLeadMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ConsultingLead save(ConsultingLead lead) {
        repository.save(mapper.toEntity(lead));
        return lead;
    }
}
