package io.opencertflow.diagnosis.adapter.out.persistence.schema;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code product_group_question_override} 리포지토리. */
public interface ProductGroupQuestionOverrideJpaRepository
        extends JpaRepository<ProductGroupQuestionOverrideEntity, Long> {

    List<ProductGroupQuestionOverrideEntity> findByProductGroup(String productGroup);

    Optional<ProductGroupQuestionOverrideEntity> findByProductGroupAndCode(
            String productGroup, String code);
}
