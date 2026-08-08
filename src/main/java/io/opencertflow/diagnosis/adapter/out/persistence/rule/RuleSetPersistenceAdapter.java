package io.opencertflow.diagnosis.adapter.out.persistence.rule;

import io.opencertflow.common.adapter.out.persistence.annotation.PersistenceAdapter;
import io.opencertflow.diagnosis.application.port.out.LoadRuleSetPort;
import io.opencertflow.diagnosis.domain.model.ProductGroup;
import io.opencertflow.diagnosis.domain.rule.Rule;
import io.opencertflow.diagnosis.domain.rule.RuleCode;
import io.opencertflow.diagnosis.domain.rule.RuleSet;
import io.opencertflow.diagnosis.domain.rule.RuleSetVersion;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LoadRuleSetPort} 구현. 활성 룰셋을 DB에서 읽어 코덱으로 도메인 트리를 복원한다.
 *
 * <p>블로킹(JPA)이다. 호출자(서비스)는 {@code BlockingBridge}로 감싼다. 트랜잭션은 이 메서드
 * 안에서 시작하고 끝난다 — 리액티브 체인이 아니라 여기가 트랜잭션 경계다(ADR-0002).
 */
@PersistenceAdapter
public class RuleSetPersistenceAdapter implements LoadRuleSetPort {

    private final RuleSetJpaRepository repository;
    private final RuleJsonCodec codec;

    public RuleSetPersistenceAdapter(RuleSetJpaRepository repository, RuleJsonCodec codec) {
        this.repository = repository;
        this.codec = codec;
    }

    @Override
    @Transactional(readOnly = true)
    public RuleSet loadActive(ProductGroup productGroup) {
        return repository.findByProductGroupAndActiveIsTrue(productGroup.name())
                .map(this::toDomain)
                .orElse(null); // 없으면 null → BlockingBridge가 빈 Mono로 → 503
    }

    private RuleSet toDomain(RuleSetEntity entity) {
        List<Rule> rules = entity.getRules().stream()
                .map(this::toDomainRule)
                .toList();
        return new RuleSet(
                RuleSetVersion.of(entity.getVersion()),
                ProductGroup.valueOf(entity.getProductGroup()),
                rules);
    }

    private Rule toDomainRule(RuleEntity entity) {
        return new Rule(
                RuleCode.of(entity.getRuleCode()),
                entity.getPriority(),
                codec.parseCondition(entity.getCondition()),
                codec.parseEffects(entity.getEffects()));
    }
}
