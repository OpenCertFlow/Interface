package com.certimakers.consulting.adapter.out.persistence;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.consulting.application.port.out.ConsultingMessagePort;
import com.certimakers.consulting.domain.model.ConsultingMessage;
import com.certimakers.consulting.domain.model.MessageKind;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
public class ConsultingMessagePersistenceAdapter implements ConsultingMessagePort {

    private final ConsultingMessageJpaRepository repository;

    public ConsultingMessagePersistenceAdapter(ConsultingMessageJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void append(ConsultingMessage message) {
        repository.save(new ConsultingMessageEntity(
                message.id(), message.leadId(), message.authorId(),
                message.kind().name(), message.body(), message.createdAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultingMessage> findByLead(Long leadId) {
        return repository.findByLeadIdOrderByCreatedAtAsc(leadId).stream()
                .map(e -> new ConsultingMessage(
                        e.getId(), e.getLeadId(), e.getAuthorId(),
                        MessageKind.valueOf(e.getKind()), e.getBody(), e.getCreatedAt()))
                .toList();
    }
}
