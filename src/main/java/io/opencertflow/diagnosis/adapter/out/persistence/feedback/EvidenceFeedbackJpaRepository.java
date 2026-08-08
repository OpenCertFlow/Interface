package io.opencertflow.diagnosis.adapter.out.persistence.feedback;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceFeedbackJpaRepository extends JpaRepository<EvidenceFeedbackEntity, Long> {

    List<EvidenceFeedbackEntity> findAllByOrderByCreatedAtDesc();
}
