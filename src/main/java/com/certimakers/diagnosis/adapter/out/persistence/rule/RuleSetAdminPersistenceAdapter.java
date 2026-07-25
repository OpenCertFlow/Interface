package com.certimakers.diagnosis.adapter.out.persistence.rule;

import com.certimakers.common.adapter.out.persistence.annotation.PersistenceAdapter;
import com.certimakers.common.domain.port.IdGenerator;
import com.certimakers.common.domain.port.TimeProvider;
import com.certimakers.diagnosis.application.port.out.RuleSetAdminPort;
import com.certimakers.diagnosis.domain.model.ProductGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link RuleSetAdminPort} 구현. 관리자 API가 룰셋을 조회·저장·배포하는 경로다.
 *
 * <p>진단 경로({@link RuleSetPersistenceAdapter})와 달리 여기서는 저장된 JSON을 파싱하지 않고
 * 원문 그대로 다룬다 — 관리 화면은 룰을 편집·배포하지 실행하지 않기 때문이다. 파싱 검증은
 * {@link RuleDefinitionValidatorAdapter}가 별도로 담당한다.
 */
@PersistenceAdapter
public class RuleSetAdminPersistenceAdapter implements RuleSetAdminPort {

    private final RuleSetJpaRepository repository;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public RuleSetAdminPersistenceAdapter(
            RuleSetJpaRepository repository, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleSetSummary> findAllSummaries() {
        return repository.findAllByOrderByProductGroupAscVersionDesc().stream()
                .map(entity -> new RuleSetSummary(
                        entity.getId(), entity.getProductGroup(), entity.getVersion(),
                        entity.isActive(), entity.getActivatedAt(), entity.getRules().size()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RuleSetDetail> findDetail(Long ruleSetId) {
        return repository.findWithRulesById(ruleSetId).map(entity -> new RuleSetDetail(
                entity.getId(), entity.getProductGroup(), entity.getVersion(),
                entity.isActive(), entity.getActivatedAt(),
                entity.getRules().stream()
                        .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                        .map(rule -> new StoredRule(
                                rule.getRuleCode(), rule.getPriority(), rule.getCondition(),
                                rule.getEffects(), rule.getDescription()))
                        .toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public int nextVersion(ProductGroup productGroup) {
        return repository.findMaxVersion(productGroup.name()).orElse(0) + 1;
    }

    @Override
    @Transactional
    public Long saveDraft(NewRuleSet ruleSet) {
        RuleSetEntity entity = new RuleSetEntity(
                idGenerator.nextId(), ruleSet.version(), ruleSet.productGroup().name());
        for (NewRule rule : ruleSet.rules()) {
            entity.addRule(new RuleEntity(
                    idGenerator.nextId(), rule.ruleCode(), rule.priority(),
                    rule.conditionJson(), rule.effectsJson(), rule.description()));
        }
        return repository.save(entity).getId();
    }

    @Override
    @Transactional
    public boolean activate(Long ruleSetId) {
        Optional<RuleSetEntity> target = repository.findById(ruleSetId);
        if (target.isEmpty()) {
            return false;
        }
        RuleSetEntity ruleSet = target.get();

        // 같은 제품군의 기존 활성본을 먼저 끈다. 부분 유니크 인덱스(활성 하나) 위반을 피하려면
        // 활성화 전에 비활성화가 DB에 반영되어야 하므로 saveAndFlush로 순서를 강제한다.
        repository.findByProductGroupAndActiveIsTrue(ruleSet.getProductGroup())
                .filter(current -> !current.getId().equals(ruleSet.getId()))
                .ifPresent(current -> {
                    current.deactivate();
                    repository.saveAndFlush(current);
                });

        ruleSet.activate(timeProvider.now());
        repository.save(ruleSet);
        return true;
    }
}
