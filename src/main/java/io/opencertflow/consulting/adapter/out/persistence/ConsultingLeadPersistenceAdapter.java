package io.opencertflow.consulting.adapter.out.persistence;

import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.common.domain.error.BusinessException;
import io.opencertflow.consulting.application.port.out.LoadConsultingLeadsPort;
import io.opencertflow.consulting.application.port.out.PurgeLeadsPort;
import io.opencertflow.consulting.application.port.out.SaveConsultingLeadPort;
import io.opencertflow.consulting.application.port.out.UpdateConsultingLeadPort;
import io.opencertflow.consulting.domain.error.ConsultingErrorCode;
import io.opencertflow.consulting.domain.model.ConsultingLead;
import io.opencertflow.consulting.domain.model.ConsultingLeadId;
import io.opencertflow.consulting.domain.model.LeadStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 컨설팅 리드 저장·조회·워크플로 갱신. 연락처 암·복호화는 매퍼가 수행한다.
 *
 * <p>저장(save)은 리드·동의를 함께 넣는 접수 경로, 갱신(update)은 기존 행의 워크플로 컬럼만 바꾸는
 * 컨설턴트 경로다 — 갱신 시 동의를 다시 만들지 않도록 분리했다.
 */
@PersistenceAdapter
public class ConsultingLeadPersistenceAdapter
        implements SaveConsultingLeadPort, LoadConsultingLeadsPort, UpdateConsultingLeadPort,
        PurgeLeadsPort {

    /** 파기 대상이 되는 종착 상태. 진행 중 리드는 보관한다. */
    private static final List<String> TERMINAL_STATUSES =
            List.of(LeadStatus.COMPLETED.name(), LeadStatus.CANCELLED.name());

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

    @Override
    @Transactional(readOnly = true)
    public List<ConsultingLead> findLeads(String statusFilter, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        List<ConsultingLeadEntity> entities = statusFilter == null || statusFilter.isBlank()
                ? repository.findByOrderByCreatedAtDesc(page)
                : repository.findByStatusOrderByCreatedAtDesc(statusFilter, page);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConsultingLead> findLead(ConsultingLeadId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultingLead> findByOwner(String ownerUserId, int limit) {
        return repository
                .findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void update(ConsultingLead lead) {
        ConsultingLeadEntity entity = repository.findById(lead.id().value())
                .orElseThrow(() -> new BusinessException(ConsultingErrorCode.LEAD_NOT_FOUND));
        entity.applyWorkflow(
                lead.status().name(),
                lead.assignedConsultantId().orElse(null),
                lead.internalMemo().orElse(null));
        repository.save(entity);
    }

    @Override
    @Transactional
    public long deleteTerminalOlderThan(Instant threshold) {
        return repository.deleteByStatusInAndCreatedAtBefore(TERMINAL_STATUSES, threshold);
    }
}
