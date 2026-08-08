package io.opencertflow.report.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportPhraseJpaRepository extends JpaRepository<ReportPhraseEntity, String> {

    List<ReportPhraseEntity> findAllByOrderByPhraseKeyAsc();
}
